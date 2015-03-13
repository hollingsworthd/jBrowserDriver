/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Permission;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.HashSet;
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
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import sun.net.www.MessageHeader;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;

class StreamConnection extends HttpURLConnection {
  private static final Object lock = new Object();
  private static SSLSocketFactory socketFactory;
  private static long lastCertUpdate;
  //a good pem source: https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt
  private static final String pemFile = System.getProperty("jbd.pemfile");
  private final HttpURLConnection conn;
  private final boolean isSsl;
  private final AtomicBoolean skip = new AtomicBoolean();
  private static final Pattern downloadHeader = Pattern.compile(
      "^\\s*attachment\\s*(?:;\\s*filename\\s*=\\s*[\"']?\\s*(.*?)\\s*[\"']?\\s*)?", Pattern.CASE_INSENSITIVE);
  private final Object connObjDelegate;
  private final AtomicLong settingsId = new AtomicLong();
  private static final Field headerField;
  private static final Field cookieHandlerField;
  private static final Set<String> adHosts = new HashSet<String>();
  private static final URL dummy;
  static {
    Field headerFieldTmp = null;
    try {
      headerFieldTmp = sun.net.www.protocol.http.HttpURLConnection.class.getDeclaredField("requests");
      headerFieldTmp.setAccessible(true);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    headerField = headerFieldTmp;

    Field cookieHandlerFieldTmp = null;
    try {
      cookieHandlerFieldTmp = sun.net.www.protocol.http.HttpURLConnection.class.getDeclaredField("cookieHandler");
      cookieHandlerFieldTmp.setAccessible(true);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    cookieHandlerField = cookieHandlerFieldTmp;

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

    URL dummyTmp = null;
    try {
      URI.create("about:blank").toURL();
    } catch (Throwable t) {}
    dummy = dummyTmp;
  }
  private static Pattern pemBlock = Pattern.compile(
      "-----BEGIN CERTIFICATE-----\\s*(.*?)\\s*-----END CERTIFICATE-----", Pattern.DOTALL);

  private static SSLSocketFactory updatedSocketFactory() {
    synchronized (lock) {
      if (pemFile != null && System.currentTimeMillis() - lastCertUpdate > 48 * 60 * 60 * 1000) {
        try {
          String location = pemFile;
          File cachedPemFile = new File("./pemfile_cached");
          boolean remote = location.startsWith("https://") || location.startsWith("http://");
          if (remote && cachedPemFile.exists()
              && (System.currentTimeMillis() - cachedPemFile.lastModified() < 48 * 60 * 60 * 1000)) {
            location = cachedPemFile.getAbsolutePath();
            remote = false;
          }
          String pemBlocks = null;
          if (remote) {
            HttpURLConnection remotePemFile = StreamHandler.defaultConnection(location);
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
            lastCertUpdate = System.currentTimeMillis();
            socketFactory = context.getSocketFactory();
            return socketFactory;
          }
        } catch (Throwable t) {
          Logs.exception(t);
        }
      }
      return socketFactory;
    }
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

  StreamConnection(HttpsURLConnectionImpl conn) throws IOException {
    super(dummy);
    this.conn = conn;
    this.isSsl = true;
    SSLSocketFactory socketFactory = updatedSocketFactory();
    if (socketFactory != null) {
      conn.setSSLSocketFactory(socketFactory);
    }
    Object connObjDelegateTmp = null;
    Field field = null;
    try {
      field = HttpsURLConnectionImpl.class.getDeclaredField("delegate");
      field.setAccessible(true);
      connObjDelegateTmp = field.get(conn);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    connObjDelegate = connObjDelegateTmp;
  }

  StreamConnection(sun.net.www.protocol.http.HttpURLConnection conn)
      throws IOException {
    super(dummy);
    this.conn = conn;
    this.isSsl = false;
    connObjDelegate = conn;
  }

  @Override
  public void connect() throws IOException {
    try {
      StreamHeader header =
          new StreamHeader(this, (MessageHeader) headerField.get(connObjDelegate), connObjDelegate, isSsl);
      headerField.set(connObjDelegate, header);
      settingsId.set(header.settingsId().get());
      cookieHandlerField.set(connObjDelegate, SettingsManager.get(settingsId.get()).get().cookieManager());
    } catch (Throwable t) {
      Logs.exception(t);
    }
    if (StatusMonitor.get(settingsId.get()).isDiscarded(conn.getURL().toExternalForm())
        || isBlocked(conn.getURL().getHost())) {
      conn.setDoInput(false);
      skip.set(true);
    } else {
      conn.connect();
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
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
          Files.write(downloadFile.toPath(), Util.toBytes(conn.getInputStream()));
        }
        skip.set(true);
      }
    }
    return skip.get() ? new ByteArrayInputStream(new byte[0])
        : StreamInjectors.injectedStream(conn, settingsId.get());
  }

  @Override
  public int getResponseCode() throws IOException {
    return skip.get() ? 204 : conn.getResponseCode();
  }

  @Override
  public String getContentEncoding() {
    return skip.get() ? null : conn.getContentEncoding();
  }

  @Override
  public int getContentLength() {
    return skip.get() ? 0 : conn.getContentLength();
  }

  @Override
  public long getContentLengthLong() {
    return skip.get() ? 0l : conn.getContentLengthLong();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return skip.get() ? new ByteArrayOutputStream() : conn.getOutputStream();
  }

  @Override
  public void setRequestProperty(String arg0, String arg1) {
    conn.setRequestProperty(arg0, arg1);
  }

  @Override
  public void addRequestProperty(String arg0, String arg1) {
    conn.addRequestProperty(arg0, arg1);
  }

  @Override
  public String getRequestProperty(String arg0) {
    return conn.getRequestProperty(arg0);
  }

  @Override
  public Map<String, List<String>> getRequestProperties() {
    return conn.getRequestProperties();
  }

  @Override
  public void disconnect() {
    conn.disconnect();
  }

  @Override
  public InputStream getErrorStream() {
    return conn.getErrorStream();
  }

  @Override
  public String getHeaderField(int arg0) {
    return conn.getHeaderField(arg0);
  }

  @Override
  public long getHeaderFieldDate(String arg0, long arg1) {
    return conn.getHeaderFieldDate(arg0, arg1);
  }

  @Override
  public String getHeaderFieldKey(int arg0) {
    return conn.getHeaderFieldKey(arg0);
  }

  @Override
  public boolean getInstanceFollowRedirects() {
    return conn.getInstanceFollowRedirects();
  }

  @Override
  public Permission getPermission() throws IOException {
    return conn.getPermission();
  }

  @Override
  public String getRequestMethod() {
    return conn.getRequestMethod();
  }

  @Override
  public String getResponseMessage() throws IOException {
    return conn.getResponseMessage();
  }

  @Override
  public void setChunkedStreamingMode(int arg0) {
    conn.setChunkedStreamingMode(arg0);
  }

  @Override
  public void setFixedLengthStreamingMode(int arg0) {
    conn.setFixedLengthStreamingMode(arg0);
  }

  @Override
  public void setFixedLengthStreamingMode(long arg0) {
    conn.setFixedLengthStreamingMode(arg0);
  }

  @Override
  public void setInstanceFollowRedirects(boolean arg0) {
    conn.setInstanceFollowRedirects(arg0);
  }

  @Override
  public void setRequestMethod(String arg0) throws ProtocolException {
    conn.setRequestMethod(arg0);
  }

  @Override
  public boolean usingProxy() {
    return conn.usingProxy();
  }

  @Override
  public boolean getAllowUserInteraction() {
    return conn.getAllowUserInteraction();
  }

  @Override
  public int getConnectTimeout() {
    return conn.getConnectTimeout();
  }

  @Override
  public Object getContent() throws IOException {
    return conn.getContent();
  }

  @Override
  public Object getContent(Class[] arg0) throws IOException {
    return conn.getContent(arg0);
  }

  @Override
  public String getContentType() {
    return conn.getContentType();
  }

  @Override
  public long getDate() {
    return conn.getDate();
  }

  @Override
  public boolean getDefaultUseCaches() {
    return conn.getDefaultUseCaches();
  }

  @Override
  public boolean getDoInput() {
    return conn.getDoInput();
  }

  @Override
  public boolean getDoOutput() {
    return conn.getDoOutput();
  }

  @Override
  public long getExpiration() {
    return conn.getExpiration();
  }

  @Override
  public String getHeaderField(String fieldName) {
    return conn.getHeaderField(fieldName);
  }

  @Override
  public int getHeaderFieldInt(String arg0, int arg1) {
    return conn.getHeaderFieldInt(arg0, arg1);
  }

  @Override
  public long getHeaderFieldLong(String arg0, long arg1) {
    return conn.getHeaderFieldLong(arg0, arg1);
  }

  @Override
  public Map<String, List<String>> getHeaderFields() {
    return conn.getHeaderFields();
  }

  @Override
  public long getIfModifiedSince() {
    return conn.getIfModifiedSince();
  }

  @Override
  public long getLastModified() {
    return conn.getLastModified();
  }

  @Override
  public int getReadTimeout() {
    return conn.getReadTimeout();
  }

  @Override
  public URL getURL() {
    return conn.getURL();
  }

  @Override
  public boolean getUseCaches() {
    return conn.getUseCaches();
  }

  @Override
  public void setAllowUserInteraction(boolean arg0) {
    conn.setAllowUserInteraction(arg0);
  }

  @Override
  public void setConnectTimeout(int arg0) {
    conn.setConnectTimeout(arg0);
  }

  @Override
  public void setDefaultUseCaches(boolean arg0) {
    conn.setDefaultUseCaches(arg0);
  }

  @Override
  public void setDoInput(boolean arg0) {
    conn.setDoInput(arg0);
  }

  @Override
  public void setDoOutput(boolean arg0) {
    conn.setDoOutput(arg0);
  }

  @Override
  public void setIfModifiedSince(long arg0) {
    conn.setIfModifiedSince(arg0);
  }

  @Override
  public void setReadTimeout(int arg0) {
    conn.setReadTimeout(arg0);
  }

  @Override
  public void setUseCaches(boolean arg0) {
    conn.setUseCaches(arg0);
  }

  @Override
  public String toString() {
    return conn.toString();
  }

  @Override
  public boolean equals(Object arg0) {
    return conn.equals(arg0);
  }

  @Override
  public int hashCode() {
    return conn.hashCode();
  }
}