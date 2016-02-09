/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC
 * 
 * Sales and support: ops@machinepublishers.com
 * Updates: https://github.com/MachinePublishers/jBrowserDriver
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Permission;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CustomCachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.LaxCookieSpecProvider;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

class StreamConnection extends HttpURLConnection implements Closeable {
  private static final File attachmentsDir;
  private static final File mediaDir;
  private static final File cacheDir;
  static {
    File attachmentsDirTmp = null;
    File mediaDirTmp = null;
    File cacheDirTmp = SettingsManager.settings().cacheDir();
    try {
      attachmentsDirTmp = Files.createTempDirectory("jbd_attachments_").toFile();
      mediaDirTmp = Files.createTempDirectory("jbd_media_").toFile();
      cacheDirTmp = cacheDirTmp == null ? Files.createTempDirectory("jbd_webcache_").toFile() : cacheDirTmp;
      attachmentsDirTmp.deleteOnExit();
      mediaDirTmp.deleteOnExit();
      if (SettingsManager.settings().cacheDir() == null) {
        final File finalCacheDir = cacheDirTmp;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
          @Override
          public void run() {
            FileUtils.deleteQuietly(finalCacheDir);
          }
        }));
      } else {
        cacheDirTmp.mkdirs();
      }
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    }
    attachmentsDir = attachmentsDirTmp;
    mediaDir = mediaDirTmp;
    cacheDir = cacheDirTmp;
  }
  private static final Set<String> ignoredHeaders = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
      "cookie", "pragma", "cache-control", "content-length" })));
  private static final Pattern invalidUrlChar = Pattern.compile("[^-A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=]");
  private static Pattern pemBlock = Pattern.compile(
      "-----BEGIN CERTIFICATE-----\\s*(.*?)\\s*-----END CERTIFICATE-----", Pattern.DOTALL);
  private static final Set<String> adHosts = new HashSet<String>();
  private static final Pattern downloadHeader = Pattern.compile(
      "^\\s*attachment\\s*(?:;\\s*filename\\s*=\\s*[\"']?\\s*(.*?)\\s*[\"']?\\s*)?", Pattern.CASE_INSENSITIVE);
  private static final int ROUTE_CONNECTIONS = Integer.parseInt(System.getProperty("jbd.maxrouteconnections", "8"));
  private static final int CONNECTIONS = Integer.parseInt(
      System.getProperty("jbd.maxconnections", "3000"));
  private static final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
      .register("https", new SslSocketFactory(sslContext()))
      .register("http", new SocketFactory())
      .build();
  private static final PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(registry);

  static {
    manager.setDefaultMaxPerRoute(ROUTE_CONNECTIONS);
    manager.setMaxTotal(CONNECTIONS);
  }
  private static final Registry<CookieSpecProvider> cookieProvider = RegistryBuilder.<CookieSpecProvider> create()
      .register("custom", new LaxCookieSpecProvider())
      .build();
  private static final CloseableHttpClient client = HttpClients.custom()
      .disableRedirectHandling()
      .disableAutomaticRetries()
      .setDefaultCookieSpecRegistry(cookieProvider)
      .setConnectionManager(manager)
      .setMaxConnPerRoute(ROUTE_CONNECTIONS)
      .setMaxConnTotal(CONNECTIONS)
      .setDefaultCredentialsProvider(ProxyAuth.instance())
      .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
      .build();
  private static final CacheConfig cacheConfig = CacheConfig.custom()
      .setMaxCacheEntries(SettingsManager.settings().cacheEntries())
      .setMaxObjectSize(SettingsManager.settings().cacheEntrySize())
      .build();
  private static final CloseableHttpClient cachingClient = CustomCachingHttpClientBuilder.create()
      .setCacheConfig(cacheConfig)
      .setHttpCacheStorage(new HttpCache(cacheDir))
      .disableRedirectHandling()
      .disableAutomaticRetries()
      .setDefaultCookieSpecRegistry(cookieProvider)
      .setConnectionManager(manager)
      .setMaxConnPerRoute(ROUTE_CONNECTIONS)
      .setMaxConnTotal(CONNECTIONS)
      .setDefaultCredentialsProvider(ProxyAuth.instance())
      .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
      .build();

  private final Map<String, List<String>> reqHeaders = new LinkedHashMap<String, List<String>>();
  private final Map<String, String> reqHeadersCasing = new HashMap<String, String>();
  private final AtomicReference<RequestConfig.Builder> config = new AtomicReference<RequestConfig.Builder>(RequestConfig.custom());
  private final URL url;
  private final String urlString;
  private final AtomicBoolean skip = new AtomicBoolean();
  private final AtomicInteger connectTimeout = new AtomicInteger();
  private final AtomicInteger readTimeout = new AtomicInteger();
  private final AtomicReference<String> method = new AtomicReference<String>();
  private final AtomicBoolean cache = new AtomicBoolean(SettingsManager.settings().cache());
  private final AtomicBoolean connected = new AtomicBoolean();
  private final AtomicBoolean exec = new AtomicBoolean();
  private final AtomicReference<CloseableHttpResponse> response = new AtomicReference<CloseableHttpResponse>();
  private final AtomicReference<HttpEntity> entity = new AtomicReference<HttpEntity>();
  private final AtomicBoolean consumed = new AtomicBoolean();
  private final AtomicBoolean closed = new AtomicBoolean();
  private final AtomicReference<HttpClientContext> context = new AtomicReference<HttpClientContext>(HttpClientContext.create());
  private final AtomicReference<HttpRequestBase> req = new AtomicReference<HttpRequestBase>();
  private final AtomicBoolean contentEncodingRemoved = new AtomicBoolean();
  private final AtomicLong contentLength = new AtomicLong(-1);
  private final AtomicReference<ByteArrayOutputStream> reqData = new AtomicReference<ByteArrayOutputStream>(new ByteArrayOutputStream());

  static {
    if (!"false".equals(System.getProperty("jbd.blockads"))) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(
            new InputStreamReader(StreamConnection.class.getResourceAsStream("/com/machinepublishers/jbrowserdriver/ad-hosts.txt")));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
          adHosts.add(line);
        }
      } catch (Throwable t) {
        LogsServer.instance().exception(t);
      } finally {
        Util.close(reader);
      }
    }
  }

  private static SSLContext sslContext() {
    final String property = System.getProperty("jbd.pemfile");
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
            HttpURLConnection remotePemFile = (HttpURLConnection) new URL(location).openConnection();
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

  static File cacheDir() {
    return cacheDir;
  }

  static File attachmentsDir() {
    return attachmentsDir;
  }

  static File mediaDir() {
    return mediaDir;
  }

  private boolean isBlocked(String host) {
    if (!adHosts.isEmpty()) {
      host = host.toLowerCase();
      while (host.contains(".")) {
        if (adHosts.contains(host)) {
          LogsServer.instance().trace("Ad blocked: " + host);
          host = null;
          return true;
        }
        host = host.substring(host.indexOf('.') + 1);
      }
      host = null;
    }
    return false;
  }

  boolean isMedia() {
    String contentType = getContentType();
    return contentType == null
        || contentType.isEmpty()
        || contentType.startsWith("image/")
        || contentType.startsWith("video/")
        || contentType.startsWith("audio/")
        || contentType.startsWith("model/")
        || contentType.startsWith("font/")
        || contentType.startsWith("application/octet-stream")
        || contentType.contains("/font-")
        || contentType.contains("/vnd.")
        || contentType.contains("/x.");
  }

  StreamConnection(URL url) throws MalformedURLException {
    super(url);
    this.url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
    this.urlString = url.toExternalForm();
  }

  private void processHeaders(Settings settings, HttpRequestBase req, String host) {
    boolean https = urlString.toLowerCase().startsWith("https://");
    Collection<String> names = settings.headers().headerNames(https);
    for (String name : names) {
      final String nameProperCase = settings.headers().nameFromLowercase(name, https);
      List<String> valuesIn = reqHeaders.get(name);
      String valueSettings = settings.headers().headerValue(name, https);
      if (valueSettings.equals(RequestHeaders.DROP_HEADER)) {
        continue;
      }
      if (valueSettings.equals(RequestHeaders.DYNAMIC_HEADER)) {
        if (name.equals("user-agent") && valuesIn != null && !valuesIn.isEmpty()) {
          req.addHeader(nameProperCase, settings.userAgentString());
        } else if (name.equals("host")) {
          req.addHeader(nameProperCase, host);
        } else if (valuesIn != null && !valuesIn.isEmpty()) {
          for (String curVal : valuesIn) {
            req.addHeader(nameProperCase, curVal);
          }
        }
      } else {
        req.addHeader(nameProperCase, valueSettings);
      }
    }
    for (Map.Entry<String, List<String>> entry : reqHeaders.entrySet()) {
      if (!names.contains(entry.getKey())) {
        for (String val : entry.getValue()) {
          req.addHeader(reqHeadersCasing.get(entry.getKey()), val);
        }
      }
    }
  }

  ///////////////////////////////////////////////////////////
  // Connection Functionality
  ///////////////////////////////////////////////////////////

  /**
   * {@inheritDoc}
   */
  @Override
  public void connect() throws IOException {
    try {
      if (connected.compareAndSet(false, true)) {
        if (StatusMonitor.instance().isDiscarded(urlString)
            || isBlocked(url.getHost())) {
          skip.set(true);
        } else if (SettingsManager.settings() != null) {
          config.get()
              .setCookieSpec("custom")
              .setConnectTimeout(connectTimeout.get())
              .setConnectionRequestTimeout(readTimeout.get());
          URI uri = null;
          try {
            uri = url.toURI();
          } catch (URISyntaxException e) {
            String urlString = url.toExternalForm();
            Matcher matcher = invalidUrlChar.matcher(urlString);
            StringBuilder builder = new StringBuilder();
            int left = 0;
            while (matcher.find()) {
              builder.append(urlString.substring(left, matcher.start()));
              builder.append(URLEncoder.encode(matcher.group(), "utf-8"));
              left = matcher.start() + 1;
            }
            builder.append(urlString.substring(left));
            uri = new URI(builder.toString());
          }
          if ("OPTIONS".equals(method.get())) {
            req.set(new HttpOptions(uri));
          } else if ("GET".equals(method.get())) {
            req.set(new HttpGet(uri));
          } else if ("HEAD".equals(method.get())) {
            req.set(new HttpHead(uri));
          } else if ("POST".equals(method.get())) {
            req.set(new HttpPost(uri));
          } else if ("PUT".equals(method.get())) {
            req.set(new HttpPut(uri));
          } else if ("DELETE".equals(method.get())) {
            req.set(new HttpDelete(uri));
          } else if ("TRACE".equals(method.get())) {
            req.set(new HttpTrace(uri));
          }
          processHeaders(SettingsManager.settings(), req.get(), url.getHost());
          ProxyConfig proxy = SettingsManager.settings().proxy();
          if (proxy != null && !proxy.directConnection()) {
            config.get().setExpectContinueEnabled(proxy.expectContinue());
            InetSocketAddress proxyAddress = new InetSocketAddress(proxy.host(), proxy.port());
            if (proxy.type() == ProxyConfig.Type.SOCKS) {
              context.get().setAttribute("proxy.socks.address", proxyAddress);
            } else {
              config.get().setProxy(new HttpHost(proxy.host(), proxy.port()));
            }
          }
          context.get().setCookieStore(SettingsManager.settings().cookieStore());
          context.get().setRequestConfig(config.get().build());
          StatusMonitor.instance().addStatusMonitor(url, this);
        }
      }
    } catch (Throwable t) {
      throw new IOException(t.getMessage() + ": " + urlString, t);
    }
  }

  private void exec() throws IOException {
    try {
      if (exec.compareAndSet(false, true)) {
        connect();
        if (req.get() != null) {
          if ("POST".equals(method.get())) {
            ((HttpPost) req.get()).setEntity(new ByteArrayEntity(reqData.get().toByteArray()));
          } else if ("PUT".equals(method.get())) {
            ((HttpPut) req.get()).setEntity(new ByteArrayEntity(reqData.get().toByteArray()));
          }
          response.set(cache.get() ? cachingClient.execute(req.get(), context.get()) : client.execute(req.get(), context.get()));
          if (response.get() != null && response.get().getEntity() != null) {
            entity.set(response.get().getEntity());
            if (!cache.get()) {
              response.get().setHeader("Cache-Control", "no-store");
            }
          }
        }
      }
    } catch (Throwable t) {
      throw new IOException(t.getMessage() + ": " + urlString, t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect() {
    //Do nothing. Let this lib and the underlying lib handle this.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    if (closed.compareAndSet(false, true)) {
      if (entity.get() != null) {
        try {
          EntityUtils.consume(entity.get());
        } catch (Throwable t) {}
      }
      if (req.get() != null) {
        try {
          req.get().reset();
        } catch (Throwable t) {}
      }
      if (response.get() != null) {
        try {
          response.get().close();
        } catch (Throwable t) {}
      }
    }
  }

  static void cleanUp() {
    manager.closeExpiredConnections();
    manager.closeIdleConnections(30, TimeUnit.SECONDS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream getInputStream() throws IOException {
    exec();
    if (consumed.compareAndSet(false, true)) {
      if (entity.get() != null) {
        try {
          InputStream entityStream = entity.get().getContent();
          if (entityStream != null && !skip.get()) {
            String header = getHeaderField("Content-Disposition");
            if (header != null && !header.isEmpty()) {
              Matcher matcher = downloadHeader.matcher(header);
              if (matcher.matches()) {
                Settings settings = SettingsManager.settings();
                if (settings != null && settings.saveAttachments()) {
                  File downloadFile = new File(attachmentsDir,
                      matcher.group(1) == null || matcher.group(1).isEmpty()
                          ? Long.toString(System.nanoTime()) : matcher.group(1));
                  downloadFile.deleteOnExit();
                  Files.write(downloadFile.toPath(), Util.toBytes(entityStream));
                }
                skip.set(true);
              }
            }
            if (!skip.get()) {
              return StreamInjectors.injectedStream(this, entityStream, urlString);
            }
          }
        } finally {
          close();
        }
      }
    }
    return new ByteArrayInputStream(new byte[0]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream getErrorStream() {
    try {
      if (getResponseCode() > 399) {
        return getInputStream();
      }
    } catch (IOException e) {
      LogsServer.instance().exception(e);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getResponseMessage() throws IOException {
    exec();
    return response.get() == null || response.get().getStatusLine() == null ? null : response.get().getStatusLine().getReasonPhrase();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getResponseCode() throws IOException {
    exec();
    StatusMonitor.instance().addRedirect(urlString, getHeaderField("Location"));
    if (skip.get()) {
      return 204;
    }
    return response.get() == null || response.get().getStatusLine() == null ? 499 : response.get().getStatusLine().getStatusCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getContent() throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getContent(Class[] classes) throws IOException {
    throw new UnsupportedOperationException();
  }

  ///////////////////////////////////////////////////////////
  // Response Attributes
  ///////////////////////////////////////////////////////////

  /**
   * {@inheritDoc}
   */
  @Override
  public String getContentEncoding() {
    if (contentEncodingRemoved.get()) {
      return null;
    }
    return entity.get() == null || entity.get().getContentEncoding() == null || skip.get() ? null : entity.get().getContentEncoding().getValue();
  }

  public void removeContentEncoding() {
    response.get().removeHeaders("Content-Encoding");
    contentEncodingRemoved.set(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getContentLength() {
    if (contentLength.get() != -1) {
      return (int) contentLength.get();
    }
    return entity.get() == null || skip.get() ? 0 : (int) entity.get().getContentLength();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getContentLengthLong() {
    if (contentLength.get() != -1) {
      return contentLength.get();
    }
    return entity.get() == null || skip.get() ? 0 : entity.get().getContentLength();
  }

  public void setContentLength(long contentLength) {
    this.contentLength.set(contentLength);
    response.get().setHeader("Content-Length", Long.toString(contentLength));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Permission getPermission() throws IOException {
    //TODO
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getContentType() {
    return entity.get() == null || entity.get().getContentType() == null || skip.get() ? null : entity.get().getContentType().getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getDate() {
    return getHeaderFieldLong("Date", 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getExpiration() {
    return getHeaderFieldLong("Expires", 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLastModified() {
    return getHeaderFieldLong("Last-Modified", 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, List<String>> getHeaderFields() {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    if (response.get() != null) {
      Header[] headers = response.get().getAllHeaders();
      for (int i = 0; headers != null && i < headers.length; i++) {
        String name = headers[i].getName();
        if (!map.containsKey(name)) {
          map.put(name, new ArrayList<String>());
        }
        map.get(name).add(headers[i].getValue());
      }
    }
    return map;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getHeaderField(String name) {
    if (response.get() != null) {
      Header[] headers = response.get().getHeaders(name);
      if (headers != null && headers.length > 0) {
        return headers[headers.length - 1].getValue();
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHeaderFieldInt(String name, int defaultValue) {
    if (response.get() != null) {
      Header[] headers = response.get().getHeaders(name);
      if (headers != null && headers.length > 0) {
        return Integer.parseInt(headers[headers.length - 1].getValue());
      }
    }
    return defaultValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getHeaderFieldLong(String name, long defaultValue) {
    if (response.get() != null) {
      Header[] headers = response.get().getHeaders(name);
      if (headers != null && headers.length > 0) {
        return Long.parseLong(headers[headers.length - 1].getValue());
      }
    }
    return defaultValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getHeaderFieldDate(String name, long defaultValue) {
    return getHeaderFieldLong(name, defaultValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getHeaderFieldKey(int n) {
    return response.get() == null
        || response.get().getAllHeaders() == null
        || n >= response.get().getAllHeaders().length
        || response.get().getAllHeaders()[n] == null ? null : response.get().getAllHeaders()[n].getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getHeaderField(int n) {
    return response.get() == null
        || response.get().getAllHeaders() == null
        || n >= response.get().getAllHeaders().length
        || response.get().getAllHeaders()[n] == null ? null : response.get().getAllHeaders()[n].getValue();
  }

  ///////////////////////////////////////////////////////////
  // Request Attributes
  ///////////////////////////////////////////////////////////

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputStream getOutputStream() throws IOException {
    return skip.get() ? new ByteArrayOutputStream() : reqData.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URL getURL() {
    return url;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRequestMethod() {
    return method.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setRequestMethod(String method) throws ProtocolException {
    this.method.set(method.toUpperCase());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getConnectTimeout() {
    return connectTimeout.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setConnectTimeout(int timeout) {
    this.connectTimeout.set(timeout);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getReadTimeout() {
    return readTimeout.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setReadTimeout(int timeout) {
    this.readTimeout.set(timeout);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean usingProxy() {
    ProxyConfig proxy = SettingsManager.settings().proxy();
    return proxy != null && !proxy.directConnection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getIfModifiedSince() {
    return getRequestProperty("If-Modified-Since") == null ? 0 : Long.parseLong(getRequestProperty("If-Modified-Since"));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIfModifiedSince(long ifmodifiedsince) {
    setRequestProperty("If-Modified-Since", Long.toString(ifmodifiedsince));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, List<String>> getRequestProperties() {
    return reqHeaders;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRequestProperty(String key) {
    final String keyLowercase = key.toLowerCase();
    return reqHeaders.get(keyLowercase) == null || reqHeaders.get(keyLowercase).isEmpty()
        ? null : reqHeaders.get(keyLowercase).get(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setRequestProperty(String key, String value) {
    final String keyLowercase = key.toLowerCase();
    if (!ignoredHeaders.contains(keyLowercase)) {
      reqHeaders.remove(keyLowercase);
      List<String> list = new ArrayList<String>();
      list.add(value);
      reqHeaders.put(keyLowercase, list);
      reqHeadersCasing.put(keyLowercase, key);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addRequestProperty(String key, String value) {
    final String keyLowercase = key.toLowerCase();
    if (!ignoredHeaders.contains(keyLowercase)) {
      if (reqHeaders.get(keyLowercase) == null) {
        reqHeaders.put(keyLowercase, new ArrayList<String>());
      }
      reqHeaders.get(keyLowercase).add(value);
      reqHeadersCasing.put(keyLowercase, key);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFixedLengthStreamingMode(int contentLength) {
    //Do nothing. Let HTTP lib handle this.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFixedLengthStreamingMode(long contentLength) {
    //Do nothing. Let HTTP lib handle this.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setChunkedStreamingMode(int chunklen) {
    //Do nothing. Let HTTP lib handle this.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getAllowUserInteraction() {
    //Always allow interaction
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAllowUserInteraction(boolean allowuserinteraction) {
    //Always allow interaction
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getDoInput() {
    //Always allow input
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDoInput(boolean doinput) {
    //Always allow input
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getDoOutput() {
    //Always allow output
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDoOutput(boolean dooutput) {
    //Always allow output
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getInstanceFollowRedirects() {
    //Never follow redirects. JavaFX handles them.
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setInstanceFollowRedirects(boolean followRedirects) {
    //Never follow redirects. JavaFX handles them.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getDefaultUseCaches() {
    return cache.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDefaultUseCaches(boolean defaultusecaches) {
    //Caching is configured by Settings. Ignore this call.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getUseCaches() {
    return cache.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setUseCaches(boolean usecaches) {
    //Caching is configured by Settings. Ignore this call.
  }
}