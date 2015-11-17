/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
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
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Permission;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;

class StreamConnection extends HttpURLConnection implements Closeable {
  private static Pattern pemBlock = Pattern.compile(
      "-----BEGIN CERTIFICATE-----\\s*(.*?)\\s*-----END CERTIFICATE-----", Pattern.DOTALL);
  private static final Set<String> adHosts = new HashSet<String>();
  private static final Pattern downloadHeader = Pattern.compile(
      "^\\s*attachment\\s*(?:;\\s*filename\\s*=\\s*[\"']?\\s*(.*?)\\s*[\"']?\\s*)?", Pattern.CASE_INSENSITIVE);
  private static final int ROUTE_CONNECTIONS =
      Integer.parseInt(System.getProperty("jbd.maxrouteconnections", "16"));
  private static final int CONNECTIONS =
      Integer.parseInt(System.getProperty("jbd.maxconnections", Integer.toString(Integer.MAX_VALUE)));
  private static final Registry<ConnectionSocketFactory> registry =
      RegistryBuilder.<ConnectionSocketFactory> create()
          .register("https", new SslSocketFactory(sslContext()))
          .register("http", new SocketFactory())
          .build();
  private static final PoolingHttpClientConnectionManager manager =
      new PoolingHttpClientConnectionManager(registry);
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
      .build();
  private static final CloseableHttpClient cachingClient = CachingHttpClients.custom()
      .disableRedirectHandling()
      .disableAutomaticRetries()
      .setConnectionManager(manager)
      .setMaxConnPerRoute(ROUTE_CONNECTIONS)
      .setMaxConnTotal(CONNECTIONS)
      .setDefaultCredentialsProvider(ProxyAuth.instance())
      .build();
  private static boolean cacheByDefault;

  private final Map<String, List<String>> reqHeaders = new LinkedHashMap<String, List<String>>();
  private final RequestConfig.Builder config = RequestConfig.custom();
  private final URL url;
  private final String urlString;
  private final AtomicBoolean skip = new AtomicBoolean();
  private final AtomicLong settingsId = new AtomicLong();
  private int connectTimeout;
  private int readTimeout;
  private String method;
  private boolean cache = cacheByDefault;
  private boolean connected;
  private boolean exec;
  private CloseableHttpResponse response;
  private HttpEntity entity;
  private boolean consumed;
  private HttpClientContext context = HttpClientContext.create();
  private HttpRequestBase req;
  private HttpHost host;
  private boolean contentEncodingRemoved;
  private long contentLength = -1;
  private ByteArrayOutputStream reqData = new ByteArrayOutputStream();

  static {
    if (!"false".equals(System.getProperty("jbd.blockads"))) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(
            new InputStreamReader(StreamConnection.class.getResourceAsStream("./ad-hosts.txt")));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
          adHosts.add(line);
        }
      } catch (Throwable t) {
        Logs.exception(t);
      } finally {
        Util.close(reader);
      }
    }
  }

  private static SSLContext sslContext() {
    //a good pem source: https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt
    if (System.getProperty("jbd.pemfile") != null) {
      try {
        String location = System.getProperty("jbd.pemfile");
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
          java.security.cert.X509Certificate cert =
              (java.security.cert.X509Certificate) cf.generateCertificate(byteStream);
          String alias = cert.getSubjectX500Principal().getName("RFC2253");
          if (alias != null && !keyStore.containsAlias(alias)) {
            found = true;
            keyStore.setCertificateEntry(alias, cert);
          }
        }
        if (found) {
          KeyManagerFactory keyManager =
              KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
          keyManager.init(keyStore, null);
          TrustManagerFactory trustManager =
              TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
          trustManager.init(keyStore);
          SSLContext context = SSLContext.getInstance("TLS");
          context.init(keyManager.getKeyManagers(), trustManager.getTrustManagers(), null);
          return context;
        }
      } catch (Throwable t) {
        Logs.exception(t);
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

  private static Socket newSocket(final HttpContext context) {
    InetSocketAddress proxySocks = (InetSocketAddress) context.getAttribute("proxy.socks.address");
    InetSocketAddress proxyHttp = (InetSocketAddress) context.getAttribute("proxy.http.address");
    if (proxySocks != null) {
      return new Socket(new Proxy(Proxy.Type.SOCKS, proxySocks));
    } else if (proxyHttp != null) {
      return new Socket(new Proxy(Proxy.Type.HTTP, proxyHttp));
    }
    return new Socket();
  }

  private static boolean isBlocked(String host) {
    if (!adHosts.isEmpty()) {
      host = host.toLowerCase();
      while (host.contains(".")) {
        if (adHosts.contains(host)) {
          if (Logs.TRACE) {
            System.out.println("Ad blocked: " + host);
          }
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
    System.out.println(contentType);
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

  StreamConnection(URL url) {
    super(url);
    this.url = url;
    this.urlString = url.toExternalForm();
  }

  private void processHeaders(AtomicReference<Settings> settings, HttpRequestBase req) {
    boolean https = urlString.startsWith("https://");
    Collection<String> names = https ? settings.get().headers().namesHttps()
        : settings.get().headers().namesHttp();
    for (String name : names) {
      List<String> valuesIn = reqHeaders.get(name);
      String valueSettings = https ? settings.get().headers().headerHttps(name)
          : settings.get().headers().headerHttp(name);
      if (valueSettings == RequestHeaders.DROP_HEADER) {
        continue;
      }
      if ("Cache-Control".equalsIgnoreCase(name)
          && valuesIn != null
          && !valuesIn.isEmpty()
          && "no-cache".equals(valuesIn.get(0))) {
        //JavaFX initially sets Cache-Control to no-cache but real browsers don't 
        reqHeaders.remove("Cache-Control");
      }
      if (valueSettings == RequestHeaders.DYNAMIC_HEADER) {
        if (valuesIn != null && !valuesIn.isEmpty()) {
          if (name.equalsIgnoreCase("User-Agent")) {
            req.addHeader(name, settings.get().userAgentString());
          } else {
            for (String curVal : valuesIn) {
              req.addHeader(name, curVal);
            }
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
    if (!connected) {
      if (StatusMonitor.get(settingsId.get()).isDiscarded(urlString)
          || isBlocked(url.getHost())) {
        skip.set(true);
      } else {
        try {
          connected = true;
          config
              .setCookieSpec(CookieSpecs.STANDARD)
              .setConnectTimeout(connectTimeout)
              .setConnectionRequestTimeout(readTimeout);
          ProxyConfig proxy = SettingsManager.get(settingsId.get()).get().proxy();
          if (proxy != null && !proxy.directConnection()) {
            InetSocketAddress proxyAddress = new InetSocketAddress(proxy.host(), proxy.port());
            if (proxy.type() == ProxyConfig.Type.SOCKS) {
              context.setAttribute("proxy.socks.address", proxyAddress);
            } else {
              context.setAttribute("proxy.http.address", proxyAddress);
            }
          }
          final String file = url.getFile();
          host = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
          if ("OPTIONS".equals(method)) {
            req = new HttpOptions(file);
          } else if ("GET".equals(method)) {
            req = new HttpGet(file);
          } else if ("HEAD".equals(method)) {
            req = new HttpHead(file);
          } else if ("POST".equals(method)) {
            req = new HttpPost(file);
          } else if ("PUT".equals(method)) {
            req = new HttpPut(file);
          } else if ("DELETE".equals(method)) {
            req = new HttpDelete(file);
          } else if ("TRACE".equals(method)) {
            req = new HttpTrace(file);
          }
          processHeaders(SettingsManager.get(settingsId.get()), req);
          context.setCookieStore(SettingsManager.get(settingsId.get()).get().cookieStore());
          context.setRequestConfig(config.build());
          StatusMonitor.get(settingsId.get()).addStatusMonitor(url, this);
        } catch (Throwable t) {
          Logs.exception(t);
        }
      }
    }
  }

  private void exec() throws IOException {
    if (!exec) {
      exec = true;
      try {
        connect();
        if (req != null) {
          if ("POST".equals(method)) {
            ((HttpPost) req).setEntity(new ByteArrayEntity(reqData.toByteArray()));
          } else if ("PUT".equals(method)) {
            ((HttpPut) req).setEntity(new ByteArrayEntity(reqData.toByteArray()));
          }
          response = cache ?
              cachingClient.execute(host, req, context) : client.execute(host, req, context);
          if (response != null && response.getEntity() != null) {
            entity = response.getEntity();
          }
        }
      } catch (Throwable t) {
        Logs.exception(t);
      }
    }
  }

  @Override
  public void disconnect() {
    //Do nothing. Let this lib and the underlying lib handle this.
  }

  @Override
  public void close() throws IOException {
    if (response != null) {
      response.close();
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    exec();
    if (!consumed) {
      consumed = true;
      if (entity != null && entity.getContent() != null && !skip.get()) {
        String header = getHeaderField("Content-Disposition");
        if (header != null && !header.isEmpty()) {
          Matcher matcher = downloadHeader.matcher(header);
          if (matcher.matches()) {
            AtomicReference<Settings> settings = SettingsManager.get(settingsId.get());
            if (settings != null) {
              File downloadFile = new File(settings.get().downloadDir(),
                  matcher.group(1) == null || matcher.group(1).isEmpty()
                      ? Long.toString(System.nanoTime()) : matcher.group(1));
              downloadFile.deleteOnExit();
              Files.write(downloadFile.toPath(), Util.toBytes(entity.getContent()));
            }
            skip.set(true);
          }
        }
        if (!skip.get()) {
          return StreamInjectors.injectedStream(
              this, entity.getContent(), urlString, settingsId.get());
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
      Logs.exception(e);
    }
    return null;
  }

  @Override
  public String getResponseMessage() throws IOException {
    exec();
    return response == null || response.getStatusLine() == null ?
        null : response.getStatusLine().getReasonPhrase();
  }

  @Override
  public int getResponseCode() throws IOException {
    exec();
    StatusMonitor.get(settingsId.get()).addRedirect(
        urlString, getHeaderField("Location"));
    if (skip.get()) {
      return 204;
    }
    return response == null || response.getStatusLine() == null ?
        0 : response.getStatusLine().getStatusCode();
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
    if (contentEncodingRemoved) {
      return null;
    }
    return entity == null || entity.getContentEncoding() == null || skip.get() ?
        null : entity.getContentEncoding().getValue();
  }

  public void removeContentEncoding() {
    response.removeHeaders("content-encoding");
    contentEncodingRemoved = true;
  }

  @Override
  public int getContentLength() {
    if (contentLength != -1) {
      return (int) contentLength;
    }
    return entity == null || skip.get() ? 0 : (int) entity.getContentLength();
  }

  @Override
  public long getContentLengthLong() {
    if (contentLength != -1) {
      return contentLength;
    }
    return entity == null || skip.get() ? 0 : entity.getContentLength();
  }

  public void setContentLength(long contentLength) {
    this.contentLength = contentLength;
    response.setHeader("content-length", Long.toString(contentLength));
  }

  @Override
  public Permission getPermission() throws IOException {
    //TODO
    return null;
  }

  @Override
  public String getContentType() {
    return entity == null || entity.getContentType() == null || skip.get() ?
        null : entity.getContentType().getValue();
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
    if (response != null) {
      Header[] headers = response.getAllHeaders();
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
    if (response != null) {
      Header[] headers = response.getHeaders(name);
      if (headers != null && headers.length > 0) {
        return headers[headers.length - 1].getValue();
      }
    }
    return null;
  }

  @Override
  public int getHeaderFieldInt(String name, int defaultValue) {
    if (response != null) {
      Header[] headers = response.getHeaders(name);
      if (headers != null && headers.length > 0) {
        return Integer.parseInt(headers[headers.length - 1].getValue());
      }
    }
    return defaultValue;
  }

  @Override
  public long getHeaderFieldLong(String name, long defaultValue) {
    if (response != null) {
      Header[] headers = response.getHeaders(name);
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
    return response == null
        || response.getAllHeaders() == null
        || n >= response.getAllHeaders().length
        || response.getAllHeaders()[n] == null ?
        null : response.getAllHeaders()[n].getName();
  }

  @Override
  public String getHeaderField(int n) {
    return response == null
        || response.getAllHeaders() == null
        || n >= response.getAllHeaders().length
        || response.getAllHeaders()[n] == null ?
        null : response.getAllHeaders()[n].getValue();
  }

  ///////////////////////////////////////////////////////////
  // Request Attributes
  ///////////////////////////////////////////////////////////

  @Override
  public OutputStream getOutputStream() throws IOException {
    return skip.get() ? new ByteArrayOutputStream() : reqData;
  }

  @Override
  public URL getURL() {
    return url;
  }

  @Override
  public String getRequestMethod() {
    return method;
  }

  @Override
  public void setRequestMethod(String method) throws ProtocolException {
    this.method = method.toUpperCase();
  }

  @Override
  public int getConnectTimeout() {
    return connectTimeout;
  }

  @Override
  public void setConnectTimeout(int timeout) {
    this.connectTimeout = timeout;
  }

  @Override
  public int getReadTimeout() {
    return readTimeout;
  }

  @Override
  public void setReadTimeout(int timeout) {
    this.readTimeout = timeout;
  }

  @Override
  public boolean getDefaultUseCaches() {
    return cacheByDefault;
  }

  @Override
  public void setDefaultUseCaches(boolean defaultusecaches) {
    cacheByDefault = defaultusecaches;
  }

  @Override
  public boolean getUseCaches() {
    return cache;
  }

  @Override
  public void setUseCaches(boolean usecaches) {
    this.cache = usecaches;
  }

  @Override
  public boolean usingProxy() {
    ProxyConfig proxy = SettingsManager.get(settingsId.get()).get().proxy();
    return proxy != null && !proxy.directConnection();
  }

  @Override
  public long getIfModifiedSince() {
    return getRequestProperty("If-Modified-Since") == null ?
        0 : Long.parseLong(getRequestProperty("If-Modified-Since"));
  }

  @Override
  public void setIfModifiedSince(long ifmodifiedsince) {
    setRequestProperty("If-Modified-Since", Long.toString(ifmodifiedsince));
  }

  @Override
  public Map<String, List<String>> getRequestProperties() {
    return reqHeaders;
  }

  @Override
  public String getRequestProperty(String key) {
    return reqHeaders.get(key) == null || reqHeaders.get(key).isEmpty() ?
        null : reqHeaders.get(key).get(0);
  }

  @Override
  public void setRequestProperty(String key, String value) {
    if (key.equalsIgnoreCase("user-agent")) {
      settingsId.set(Long.parseLong(value));
    }
    reqHeaders.remove(key);
    List<String> list = new ArrayList<String>();
    list.add(value);
    reqHeaders.put(key, list);
  }

  @Override
  public void addRequestProperty(String key, String value) {
    if (key.equalsIgnoreCase("user-agent")) {
      settingsId.set(Long.parseLong(value));
    }
    if (reqHeaders.get(key) == null) {
      reqHeaders.put(key, new ArrayList<String>());
    }
    reqHeaders.get(key).add(value);
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
    //Always allow interactoin
    return true;
  }

  @Override
  public void setAllowUserInteraction(boolean allowuserinteraction) {
    //Always allow interactoin
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

}