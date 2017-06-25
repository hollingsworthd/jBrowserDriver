/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
 * https://github.com/MachinePublishers/jBrowserDriver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.machinepublishers.jbrowserdriver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

class StreamConnectionClient {
  private static final Set<String> nonCachedMethods = Collections.unmodifiableSet(new HashSet<String>(
      Arrays.asList(new String[] { "POST", "PUT", "DELETE", "PATCH" })));
  private static final Registry<CookieSpecProvider> cookieProvider = RegistryBuilder.<CookieSpecProvider> create()
      .register("custom", new LaxCookieSpecProvider())
      .build();
  private static Pattern pemBlock = Pattern.compile(
      "-----BEGIN CERTIFICATE-----\\s*(.*?)\\s*-----END CERTIFICATE-----", Pattern.DOTALL);

  private final HttpCache httpCache;
  private final File cacheDir;
  private final CacheConfig cacheConfig;
  private final Registry<ConnectionSocketFactory> registry;
  private final PoolingHttpClientConnectionManager manager;
  private final CloseableHttpClient client;
  private final CloseableHttpClient cachingClient;
  private final FileRemover shutdownHook;

  StreamConnectionClient() {
    File cacheDirTmp = SettingsManager.settings().cacheDir();
    FileRemover shutdownHookTmp = null;
    try {
      cacheDirTmp = cacheDirTmp == null ? Files.createTempDirectory("jbd_webcache_").toFile() : cacheDirTmp;
      if (SettingsManager.settings().cacheDir() == null) {
        shutdownHookTmp = new FileRemover(cacheDirTmp);
        Runtime.getRuntime().addShutdownHook(shutdownHookTmp);
      } else {
        cacheDirTmp.mkdirs();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    shutdownHook = shutdownHookTmp;
    cacheDir = cacheDirTmp;
    httpCache = new HttpCache(cacheDirTmp);

    cacheConfig = CacheConfig.custom()
        .setSharedCache(false)
        .setMaxCacheEntries(SettingsManager.settings().cacheEntries())
        .setMaxObjectSize(SettingsManager.settings().cacheEntrySize())
        .build();
    ConnectionSocketFactory sslSocketFactory = SettingsManager.settings()
        .hostnameVerification() ? new SslSocketFactory(sslContext()) : new SslSocketWithoutHostnameVerificationFactory(sslContext());
    registry = RegistryBuilder.<ConnectionSocketFactory> create()
        .register("https", sslSocketFactory)
        .register("http", new SocketFactory())
        .build();
    manager = new PoolingHttpClientConnectionManager(registry);
    manager.setDefaultMaxPerRoute(SettingsManager.settings().maxRouteConnections());
    manager.setMaxTotal(SettingsManager.settings().maxConnections());
    client = clientBuilderHelper(HttpClientBuilder.create(), manager);
    cachingClient = clientBuilderHelper(CachingHttpClientBuilder.create()
        .setCacheConfig(cacheConfig)
        .setHttpCacheStorage(httpCache),
        manager);
  }

  private static CloseableHttpClient clientBuilderHelper(HttpClientBuilder builder, PoolingHttpClientConnectionManager manager) {
    return builder
        .disableRedirectHandling()
        .disableAutomaticRetries()
        .setDefaultCookieSpecRegistry(cookieProvider)
        .setConnectionManager(manager)
        .setRequestExecutor(new HttpRequestExecutor() {
          @Override
          protected HttpResponse doSendRequest(HttpRequest request, HttpClientConnection conn, HttpContext context) throws IOException, HttpException {
            request.removeHeaders("Via");
            return super.doSendRequest(request, conn, context);
          }
        })
        .setDefaultCredentialsProvider(ProxyAuth.instance())
        .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
        .build();
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      super.finalize();
    } catch (Throwable t) {}
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      shutdownHook.run();
    } catch (Throwable t) {}
  }

  File cacheDir() {
    return cacheDir;
  }

  void cleanUp() {
    manager.closeExpiredConnections();
    manager.closeIdleConnections(30, TimeUnit.SECONDS);
  }

  void shutDown() {
    manager.shutdown();
  }

  CloseableHttpResponse execute(HttpRequestBase req, HttpClientContext context)
      throws ClientProtocolException, IOException {
    return !SettingsManager.settings().cache() || nonCachedMethods.contains(req.getMethod())
        ? client.execute(req, context) : cachingClient.execute(req, context);
  }

  private static SSLContext sslContext() {
    final String property = SettingsManager.settings().ssl();
    if (property != null && !property.isEmpty() && !"null".equals(property)) {
      if ("trustanything".equals(property)) {
        try {
          return SSLContexts.custom().loadTrustMaterial(KeyStore.getInstance(KeyStore.getDefaultType()),
              new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                  return true;
                }
              }).build();
        } catch (Throwable t) {
          LogsServer.instance().exception(t);
        }
      } else {
        try {
          String location = property;
          location = location.equals("compatible")
              ? "https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt" : location;
          File cachedPemFile = new File("./pemfile_cached");
          boolean remote = location.startsWith("https://") || location.startsWith("http://");
          if (remote && cachedPemFile.exists()
              && (System.currentTimeMillis() - cachedPemFile.lastModified() < 48 * 60 * 60 * 1000)) {
            location = cachedPemFile.getAbsolutePath();
            remote = false;
          }
          String pemBlocks = null;
          if (remote) {
            HttpURLConnection remotePemFile = (HttpURLConnection) StreamHandler.defaultConnection(new URL(location));
            remotePemFile.setRequestMethod("GET");
            remotePemFile.connect();
            pemBlocks = Util.toString(remotePemFile.getInputStream(),
                Util.charset(remotePemFile));
            cachedPemFile.delete();
            Files.write(Paths.get(cachedPemFile.getAbsolutePath()), pemBlocks.getBytes("utf-8"));
          } else {
            pemBlocks = new String(Files.readAllBytes(
                Paths.get(new File(location).getAbsolutePath())), "utf-8");
          }
          KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
          keyStore.load(null);
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          Matcher matcher = pemBlock.matcher(pemBlocks);
          boolean found = false;
          while (matcher.find()) {
            String pemBlock = matcher.group(1).replaceAll("[\\n\\r]+", "");
            ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.getDecoder().decode(pemBlock));
            java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) cf.generateCertificate(byteStream);
            String alias = cert.getSubjectX500Principal().getName("RFC2253");
            if (alias != null && !keyStore.containsAlias(alias)) {
              found = true;
              keyStore.setCertificateEntry(alias, cert);
            }
          }
          if (found) {
            KeyManagerFactory keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManager.init(keyStore, null);
            TrustManagerFactory trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManager.init(keyStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManager.getKeyManagers(), trustManager.getTrustManagers(), null);
            return context;
          }
        } catch (Throwable t) {
          LogsServer.instance().exception(t);
        }
      }
    }
    return SSLContexts.createSystemDefault();
  }

  private static class SslSocketFactory extends SSLConnectionSocketFactory {
    public SslSocketFactory(final SSLContext sslContext) {
      super(sslContext);
    }

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
      return newSocket(context);
    }
  }

  private static class SslSocketWithoutHostnameVerificationFactory extends SSLConnectionSocketFactory {
    public SslSocketWithoutHostnameVerificationFactory(final SSLContext sslContext) {
      super(sslContext, NoopHostnameVerifier.INSTANCE);
    }

    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
      return newSocket(context);
    }
  }

  private static class SocketFactory extends PlainConnectionSocketFactory {
    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
      return newSocket(context);
    }
  }

  private static Socket newSocket(final HttpContext context) throws IOException {
    InetSocketAddress proxySocks = (InetSocketAddress) context.getAttribute("proxy.socks.address");
    Socket socket;
    if (proxySocks != null) {
      socket = new Socket(new Proxy(Proxy.Type.SOCKS, proxySocks));
    } else {
      socket = new Socket();
    }
    socket.setTcpNoDelay(true);
    socket.setKeepAlive(true);
    return socket;
  }
}
