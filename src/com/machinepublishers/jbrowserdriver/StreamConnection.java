/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
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
import java.util.Base64;
import java.util.Collection;
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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
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
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

class StreamConnection extends HttpURLConnection implements Closeable {
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

  private static final CloseableHttpClient client = HttpClients.custom()
      .disableRedirectHandling()
      .disableAutomaticRetries()
      .setConnectionManager(manager)
      .setMaxConnPerRoute(ROUTE_CONNECTIONS)
      .setMaxConnTotal(CONNECTIONS)
      .setDefaultCredentialsProvider(ProxyAuth.instance())
      .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
      .build();
  private static final CloseableHttpClient cachingClient = CachingHttpClients.custom()
      .disableRedirectHandling()
      .disableAutomaticRetries()
      .setConnectionManager(manager)
      .setMaxConnPerRoute(ROUTE_CONNECTIONS)
      .setMaxConnTotal(CONNECTIONS)
      .setDefaultCredentialsProvider(ProxyAuth.instance())
      .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
      .build();
  private static final AtomicBoolean cacheByDefault = new AtomicBoolean();

  private final Map<String, List<String>> reqHeaders = new LinkedHashMap<String, List<String>>();
  private final AtomicReference<RequestConfig.Builder> config = new AtomicReference<RequestConfig.Builder>(RequestConfig.custom());
  private final URL url;
  private final String urlString;
  private final AtomicBoolean skip = new AtomicBoolean();
  private final AtomicInteger connectTimeout = new AtomicInteger();
  private final AtomicInteger readTimeout = new AtomicInteger();
  private final AtomicReference<String> method = new AtomicReference<String>();
  private final AtomicBoolean cache = new AtomicBoolean(cacheByDefault.get());
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
        Logs.instance().exception(t);
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
          Logs.instance().exception(t);
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
          Logs.instance().exception(t);
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

  private boolean isBlocked(String host) {
    if (!adHosts.isEmpty()) {
      host = host.toLowerCase();
      while (host.contains(".")) {
        if (adHosts.contains(host)) {
          Logs.instance().trace("Ad blocked: " + host);
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
    boolean https = urlString.startsWith("https://");
    Collection<String> names = https ? settings.headers().namesHttps()
        : settings.headers().namesHttp();
    for (String name : names) {
      List<String> valuesIn = reqHeaders.get(name.toLowerCase());
      String valueSettings = https ? settings.headers().headerHttps(name)
          : settings.headers().headerHttp(name);
      if (valueSettings.equals(RequestHeaders.DROP_HEADER)) {
        continue;
      }
      if (valueSettings.equals(RequestHeaders.DYNAMIC_HEADER)) {
        if (name.equalsIgnoreCase("user-agent") && valuesIn != null && !valuesIn.isEmpty()) {
          req.addHeader(name, settings.userAgentString());
        } else if (name.equalsIgnoreCase("host")) {
          req.addHeader(name, host);
        } else if (valuesIn != null && !valuesIn.isEmpty()) {
          for (String curVal : valuesIn) {
            req.addHeader(name, curVal);
          }
        }
      } else {
        req.addHeader(name, valueSettings);
      }
    }
  }

  ///////////////////////////////////////////////////////////
  // Connection Functionality
  ///////////////////////////////////////////////////////////

  @Override
  public void connect() throws IOException {
    try {
      if (connected.compareAndSet(false, true)) {
        if (StatusMonitor.instance().isDiscarded(urlString)
            || isBlocked(url.getHost())) {
          skip.set(true);
        } else if (SettingsManager.settings() != null) {
          config.get()
              .setCookieSpec(CookieSpecs.STANDARD)
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
            response.get().setHeader("cache-control", "no-store");
          }
        }
      }
    } catch (Throwable t) {
      throw new IOException(t.getMessage() + ": " + urlString, t);
    }
  }

  @Override
  public void disconnect() {
    //Do nothing. Let this lib and the underlying lib handle this.
  }

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

  @Override
  public InputStream getInputStream() throws IOException {
    exec();
    if (consumed.compareAndSet(false, true)) {
      if (entity.get() != null) {
        try {
          InputStream entityStream = entity.get().getContent();
          if (entityStream != null && !skip.get()) {
            String header = getHeaderField("content-disposition");
            if (header != null && !header.isEmpty()) {
              Matcher matcher = downloadHeader.matcher(header);
              if (matcher.matches()) {
                Settings settings = SettingsManager.settings();
                if (settings != null) {
                  File downloadFile = new File(settings.downloadDir(),
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

  @Override
  public InputStream getErrorStream() {
    try {
      if (getResponseCode() > 399) {
        return getInputStream();
      }
    } catch (IOException e) {
      Logs.instance().exception(e);
    }
    return null;
  }

  @Override
  public String getResponseMessage() throws IOException {
    exec();
    return response.get() == null || response.get().getStatusLine() == null ? null : response.get().getStatusLine().getReasonPhrase();
  }

  @Override
  public int getResponseCode() throws IOException {
    exec();
    StatusMonitor.instance().addRedirect(urlString, getHeaderField("location"));
    if (skip.get()) {
      return 204;
    }
    return response.get() == null || response.get().getStatusLine() == null ? 499 : response.get().getStatusLine().getStatusCode();
  }

  @Override
  public Object getContent() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getContent(Class[] classes) throws IOException {
    throw new UnsupportedOperationException();
  }

  ///////////////////////////////////////////////////////////
  // Response Attributes
  ///////////////////////////////////////////////////////////

  @Override
  public String getContentEncoding() {
    if (contentEncodingRemoved.get()) {
      return null;
    }
    return entity.get() == null || entity.get().getContentEncoding() == null || skip.get() ? null : entity.get().getContentEncoding().getValue();
  }

  public void removeContentEncoding() {
    response.get().removeHeaders("content-encoding");
    contentEncodingRemoved.set(true);
  }

  @Override
  public int getContentLength() {
    if (contentLength.get() != -1) {
      return (int) contentLength.get();
    }
    return entity.get() == null || skip.get() ? 0 : (int) entity.get().getContentLength();
  }

  @Override
  public long getContentLengthLong() {
    if (contentLength.get() != -1) {
      return contentLength.get();
    }
    return entity.get() == null || skip.get() ? 0 : entity.get().getContentLength();
  }

  public void setContentLength(long contentLength) {
    this.contentLength.set(contentLength);
    response.get().setHeader("content-length", Long.toString(contentLength));
  }

  @Override
  public Permission getPermission() throws IOException {
    //TODO
    return null;
  }

  @Override
  public String getContentType() {
    return entity.get() == null || entity.get().getContentType() == null || skip.get() ? null : entity.get().getContentType().getValue();
  }

  @Override
  public long getDate() {
    return getHeaderFieldLong("date", 0);
  }

  @Override
  public long getExpiration() {
    return getHeaderFieldLong("expires", 0);
  }

  @Override
  public long getLastModified() {
    return getHeaderFieldLong("last-modified", 0);
  }

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

  @Override
  public long getHeaderFieldDate(String name, long defaultValue) {
    return getHeaderFieldLong(name, defaultValue);
  }

  @Override
  public String getHeaderFieldKey(int n) {
    return response.get() == null
        || response.get().getAllHeaders() == null
        || n >= response.get().getAllHeaders().length
        || response.get().getAllHeaders()[n] == null ? null : response.get().getAllHeaders()[n].getName();
  }

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

  @Override
  public OutputStream getOutputStream() throws IOException {
    return skip.get() ? new ByteArrayOutputStream() : reqData.get();
  }

  @Override
  public URL getURL() {
    return url;
  }

  @Override
  public String getRequestMethod() {
    return method.get();
  }

  @Override
  public void setRequestMethod(String method) throws ProtocolException {
    this.method.set(method.toUpperCase());
  }

  @Override
  public int getConnectTimeout() {
    return connectTimeout.get();
  }

  @Override
  public void setConnectTimeout(int timeout) {
    this.connectTimeout.set(timeout);
  }

  @Override
  public int getReadTimeout() {
    return readTimeout.get();
  }

  @Override
  public void setReadTimeout(int timeout) {
    this.readTimeout.set(timeout);
  }

  @Override
  public boolean usingProxy() {
    ProxyConfig proxy = SettingsManager.settings().proxy();
    return proxy != null && !proxy.directConnection();
  }

  @Override
  public long getIfModifiedSince() {
    return getRequestProperty("if-modified-since") == null ? 0 : Long.parseLong(getRequestProperty("if-modified-since"));
  }

  @Override
  public void setIfModifiedSince(long ifmodifiedsince) {
    setRequestProperty("if-modified-since", Long.toString(ifmodifiedsince));
  }

  @Override
  public Map<String, List<String>> getRequestProperties() {
    return reqHeaders;
  }

  @Override
  public String getRequestProperty(String key) {
    key = key.toLowerCase();
    return reqHeaders.get(key) == null || reqHeaders.get(key).isEmpty() ? null : reqHeaders.get(key).get(0);
  }

  @Override
  public void setRequestProperty(String key, String value) {
    key = key.toLowerCase();
    if (!"cookie".equals(key)
        && !"pragma".equals(key)
        && !"cache-control".equals(key)) {
      reqHeaders.remove(key);
      List<String> list = new ArrayList<String>();
      list.add(value);
      reqHeaders.put(key, list);
    }
  }

  @Override
  public void addRequestProperty(String key, String value) {
    key = key.toLowerCase();
    if (!"cookie".equals(key)
        && !"pragma".equals(key)
        && !"cache-control".equals(key)) {
      if (reqHeaders.get(key) == null) {
        reqHeaders.put(key, new ArrayList<String>());
      }
      reqHeaders.get(key).add(value);
    }
  }

  @Override
  public void setFixedLengthStreamingMode(int contentLength) {
    //Do nothing. Let HTTP lib handle this.
  }

  @Override
  public void setFixedLengthStreamingMode(long contentLength) {
    //Do nothing. Let HTTP lib handle this.
  }

  @Override
  public void setChunkedStreamingMode(int chunklen) {
    //Do nothing. Let HTTP lib handle this.
  }

  @Override
  public boolean getAllowUserInteraction() {
    //Always allow interaction
    return true;
  }

  @Override
  public void setAllowUserInteraction(boolean allowuserinteraction) {
    //Always allow interaction
  }

  @Override
  public boolean getDoInput() {
    //Always allow input
    return true;
  }

  @Override
  public void setDoInput(boolean doinput) {
    //Always allow input
  }

  @Override
  public boolean getDoOutput() {
    //Always allow output
    return true;
  }

  @Override
  public void setDoOutput(boolean dooutput) {
    //Always allow output
  }

  @Override
  public boolean getInstanceFollowRedirects() {
    //Never follow redirects. JavaFX handles them.
    return false;
  }

  @Override
  public void setInstanceFollowRedirects(boolean followRedirects) {
    //Never follow redirects. JavaFX handles them.
  }

  @Override
  public boolean getDefaultUseCaches() {
    //Don't cache. TODO let caching be configurable.
    return true;
  }

  @Override
  public void setDefaultUseCaches(boolean defaultusecaches) {
    //Don't cache. TODO let caching be configurable.
  }

  @Override
  public boolean getUseCaches() {
    //Don't cache. TODO let caching be configurable.
    return true;
  }

  @Override
  public void setUseCaches(boolean usecaches) {
    //Don't cache. TODO let caching be configurable.
  }
}