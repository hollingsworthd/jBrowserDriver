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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;

import com.sun.webkit.network.CookieManager;

class StreamConnection extends HttpURLConnection implements Closeable {
  private static final CookieStore cookieStore = (CookieStore) CookieManager.getDefault();
  private static final File attachmentsDir;
  private static final File mediaDir;
  static {
    File attachmentsDirTmp = null;
    File mediaDirTmp = null;
    try {
      attachmentsDirTmp = Files.createTempDirectory("jbd_attachments_").toFile();
      mediaDirTmp = Files.createTempDirectory("jbd_media_").toFile();
      attachmentsDirTmp.deleteOnExit();
      mediaDirTmp.deleteOnExit();
    } catch (Throwable t) {
      Util.handleException(t);
    }
    attachmentsDir = attachmentsDirTmp;
    mediaDir = mediaDirTmp;
  }
  private static final Set<String> ignoredHeaders = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {
      "cookie", "pragma", "cache-control", "content-length" })));
  private static final Pattern invalidUrlChar = Pattern.compile("[^-A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=]");
  private static final Set<String> adHosts = new HashSet<String>();
  private static final AtomicReference<StreamConnectionClient> client = new AtomicReference<StreamConnectionClient>();
  private static final Set<String> mediaExtensions = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {
      ".svg", ".gif", ".jpeg", ".jpg", ".png",
      ".ico", ".webm", ".mp4", ".ogg", ".ogv",
      ".mp3", ".aac", ".wav", ".swf", ".woff",
      ".otf", ".ttf" })));

  private final Map<String, List<String>> reqHeaders = new LinkedHashMap<String, List<String>>();
  private final Map<String, String> reqHeadersCasing = new HashMap<String, String>();
  private final AtomicReference<RequestConfig.Builder> config = new AtomicReference<RequestConfig.Builder>(RequestConfig.custom());
  private final URL url;
  private final String urlString;
  private final String urlFragment;
  private final AtomicBoolean skip = new AtomicBoolean();
  private final AtomicReference<String> method = new AtomicReference<String>();
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
    if (SettingsManager.settings().blockAds()) {
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

  static void updateSettings() {
    StreamConnectionClient prevClient = client.get();
    if (prevClient != null) {
      prevClient.shutDown();
    }
    client.set(new StreamConnectionClient());
  }

  static File cacheDir() {
    return client.get().cacheDir();
  }

  static File attachmentsDir() {
    return attachmentsDir;
  }

  static File mediaDir() {
    return mediaDir;
  }

  private boolean isBlocked(String host) {
    if (SettingsManager.settings().blockAds() && !adHosts.isEmpty()) {
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
    String contentType = entity.get() == null || entity.get().getContentType() == null
        ? null : entity.get().getContentType().getValue();
    contentType = contentType == null ? null : contentType.toLowerCase();
    if (contentType != null
        && (contentType.startsWith("image/")
            || contentType.startsWith("video/")
            || contentType.startsWith("audio/")
            || contentType.startsWith("font/")
            || contentType.startsWith("application/octet-stream")
            || contentType.contains("/font-"))) {
      return true;
    }
    String path = url.getPath() == null ? null : url.getPath().toLowerCase();
    if (path != null) {
      for (String extension : mediaExtensions) {
        if (path.endsWith(extension)) {
          return true;
        }
      }
    }
    return false;
  }

  StreamConnection(URL url) throws MalformedURLException {
    super(url);
    this.url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
    this.urlString = url.toExternalForm();
    this.urlFragment = url.getRef();
  }

  private String hostHeader() {
    int port = url.getPort();
    return (port == -1 || port == url.getDefaultPort())
        ? url.getHost() : url.getHost() + ":" + port;
  }

  private void processHeaders(Settings settings, HttpRequestBase req) {
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
          req.addHeader(nameProperCase, hostHeader());
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
    cookieStore.addCsrfHeaders(settings, req);
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
        if (StatusMonitor.instance().isDiscarded(urlString)) {
          skip.set(true);
          LogsServer.instance().trace("Media skipped: " + urlString);
        } else if (isBlocked(url.getHost())) {
          skip.set(true);
        } else if (SettingsManager.settings() != null) {
          config.get()
              .setCookieSpec("custom")
              .setSocketTimeout(SettingsManager.settings().socketTimeout())
              .setConnectTimeout(SettingsManager.settings().connectTimeout())
              .setConnectionRequestTimeout(SettingsManager.settings().connectionReqTimeout())
              .setLocalAddress(SettingsManager.settings().getLocalIp());
          URI uri = null;
          try {
            uri = url.toURI();
          } catch (URISyntaxException e) {
            //decode components of the url first, because often the problem is partially encoded urls
            uri = new URI(url.getProtocol(),
                url.getAuthority(),
                url.getPath() == null ? null : URLDecoder.decode(url.getPath(), "utf-8"),
                url.getQuery() == null ? null : URLDecoder.decode(url.getQuery(), "utf-8"),
                url.getRef() == null ? null : URLDecoder.decode(url.getRef(), "utf-8"));
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
          } else if ("PATCH".equals(method.get())) {
            req.set(new HttpPatch(uri));
          }
          processHeaders(SettingsManager.settings(), req.get());
          ProxyConfig proxy = SettingsManager.settings().proxy();
          if (proxy != null && !proxy.directConnection() && !proxy.nonProxyHosts().contains(uri.getHost())) {
            config.get().setExpectContinueEnabled(proxy.expectContinue());
            InetSocketAddress proxyAddress = new InetSocketAddress(proxy.host(), proxy.port());
            if (proxy.type() == ProxyConfig.Type.SOCKS) {
              context.get().setAttribute("proxy.socks.address", proxyAddress);
            } else {
              config.get().setProxy(new HttpHost(proxy.host(), proxy.port()));
            }
          }
          context.get().setCookieStore(cookieStore);
          context.get().setRequestConfig(config.get().build());
          StatusMonitor.instance().monitor(url, this);
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
          } else if ("PATCH".equals(method.get())) {
            ((HttpPatch) req.get()).setEntity(new ByteArrayEntity(reqData.get().toByteArray()));
          }
          response.set(client.get().execute(req.get(), context.get()));
          if (response.get() != null && response.get().getEntity() != null) {
            entity.set(response.get().getEntity());
            response.get().setHeader("Cache-Control", "no-store, no-cache");
          }
          if (this.urlFragment != null && response.get() != null) {
            Header header = response.get().getFirstHeader("Location");
            String location = header == null ? null : header.getValue();
            if (!StringUtils.isEmpty(location)) {
              try {
                URI uri = new URIBuilder(location).build();
                if (StringUtils.isEmpty(uri.getRawFragment())) {
                  String path = uri.getPath();
                  path = StringUtils.isEmpty(path) ? "/" : path;
                  response.get().setHeader("Location",
                      new URIBuilder(location).setPath(path).setFragment(this.urlFragment).build().toString());
                }
              } catch (Throwable t) {
                if (!location.contains("#")) {
                  int fromIndex = location.indexOf("//") + 2;
                  if (fromIndex <= location.length() && location.substring(fromIndex).contains("/")) {
                    response.get().setHeader("Location",
                        new StringBuilder().append(location).append("#").append(this.urlFragment).toString());
                  } else {
                    response.get().setHeader("Location",
                        new StringBuilder().append(location).append("/#").append(this.urlFragment).toString());
                  }
                }
              }
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
    //Do nothing. Let jBrowserDriver and Apache HttpComponents handle this.
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
    }
  }

  static void cleanUp() {
    client.get().cleanUp();
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
            return ResponseHandler.handleResponse(this, entityStream);
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
    try {
      String location = getHeaderField("Location");
      if (!StringUtils.isEmpty(location)) {
        StatusMonitor.instance().addRedirect(urlString, new URL(url, location).toExternalForm());
      }
    } catch (Throwable t) {
      //ignore
    }
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
    return entity.get() == null || skip.get() ? null
        : (entity.get().getContentType() == null ? "text/html" : entity.get().getContentType().getValue());
  }

  public String getContentTypeRaw() {
    return entity.get() == null || entity.get().getContentType() == null
        ? null : entity.get().getContentType().getValue();
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
    //Apache HttpComponents handles this.
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setConnectTimeout(int timeout) {
    //Ignore. Configured by the user via Settings.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getReadTimeout() {
    //Apache HttpComponents handles this.
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setReadTimeout(int timeout) {
    //Ignore. Configured by the user via Settings.
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
    //Do nothing. Let Apache HttpComponents handle this.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFixedLengthStreamingMode(long contentLength) {
    //Do nothing. Let Apache HttpComponents handle this.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setChunkedStreamingMode(int chunklen) {
    //Do nothing. Let Apache HttpComponents handle this.
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
    //Caching is handled by Apache HttpComponents. Disable caching by JavaFX/WebKit.
    return false;
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
    //Caching is handled by Apache HttpComponents. Disable caching by JavaFX/WebKit.
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setUseCaches(boolean usecaches) {
    //Caching is configured by Settings. Ignore this call.
  }
}
