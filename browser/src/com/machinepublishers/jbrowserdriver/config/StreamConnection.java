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
package com.machinepublishers.jbrowserdriver.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import sun.net.www.MessageHeader;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;

import com.machinepublishers.jbrowserdriver.Logs;
import com.sun.org.apache.xml.internal.security.utils.Base64;

class StreamConnection extends HttpURLConnection {
  private static final Pattern httpProtocol = Pattern.compile("^https?://");
  private static final Object lock = new Object();
  private static SSLSocketFactory socketFactory;
  private static long lastCertUpdate;
  //a good pem source: https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt
  private static final String pemFile = System.getProperty("jbd.pemfile");
  private final HttpURLConnection conn;
  private final boolean isJbd;
  private final boolean isSsl;
  private final Object headerObjParent;
  private static final Field headerField;
  private final AtomicReference<Map<String, List<String>>> jbdRedirectHeaders =
      new AtomicReference<Map<String, List<String>>>();
  static {
    Field headerFieldTmp = null;
    try {
      headerFieldTmp = sun.net.www.protocol.http.HttpURLConnection.class.getDeclaredField("requests");
      headerFieldTmp.setAccessible(true);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    headerField = headerFieldTmp;
  }
  private static final URL dummy;
  static {
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
            pemBlocks = StreamHandler.toString(remotePemFile.getInputStream(),
                StreamHandler.charset(remotePemFile));
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
            ByteArrayInputStream byteStream = new ByteArrayInputStream(Base64.decode(pemBlock));
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

  public StreamConnection(HttpsURLConnectionImpl conn, boolean isJbd) throws IOException {
    super(dummy);
    this.conn = conn;
    this.isJbd = isJbd;
    this.isSsl = true;
    SSLSocketFactory socketFactory = updatedSocketFactory();
    if (socketFactory != null) {
      conn.setSSLSocketFactory(socketFactory);
    }
    Object headerObjParentTmp = null;
    Field field = null;
    try {
      field = HttpsURLConnectionImpl.class.getDeclaredField("delegate");
      field.setAccessible(true);
      headerObjParentTmp = field.get(conn);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    headerObjParent = headerObjParentTmp;
  }

  public StreamConnection(sun.net.www.protocol.http.HttpURLConnection conn, boolean isJbd)
      throws IOException {
    super(dummy);
    this.conn = conn;
    this.isJbd = isJbd;
    this.isSsl = false;
    headerObjParent = conn;
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
  public int getResponseCode() throws IOException {
    return isJbd ? conn.getResponseCode() : 307;
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
  public void connect() throws IOException {
    if (isJbd) {
      try {
        headerField.set(headerObjParent, new StreamHeader(
            conn, (MessageHeader) headerField.get(headerObjParent), headerObjParent, isSsl));
      } catch (Throwable t) {
        Logs.exception(t);
      }
      conn.connect();
    } else {
      Map<String, List<String>> jbdRedirectHeadersTmp = new LinkedHashMap<String, List<String>>();
      jbdRedirectHeadersTmp.put("Location", Arrays.asList(new String[] {
          httpProtocol.matcher(conn.getURL().toExternalForm()).replaceFirst(
              (isSsl ? "jbds" : "jbd") + conn.getRequestProperty("User-Agent") + "://") }));
      jbdRedirectHeadersTmp.put("content-length", Arrays.asList(new String[] { "0" }));
      jbdRedirectHeadersTmp.put("content-encoding", Arrays.asList(new String[] { "identity" }));
      jbdRedirectHeadersTmp.put("content-type", Arrays.asList(new String[] { "text/html" }));
      jbdRedirectHeaders.set(Collections.unmodifiableMap(jbdRedirectHeadersTmp));
    }
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
  public String getContentEncoding() {
    return isJbd ? conn.getContentEncoding()
        : jbdRedirectHeaders.get().get("content-encoding").get(0);
  }

  @Override
  public int getContentLength() {
    return isJbd ? conn.getContentLength()
        : Integer.parseInt(jbdRedirectHeaders.get().get("content-length").get(0));
  }

  @Override
  public long getContentLengthLong() {
    return isJbd ? conn.getContentLengthLong()
        : Long.parseLong(jbdRedirectHeaders.get().get("content-length").get(0));
  }

  @Override
  public String getContentType() {
    return isJbd ? conn.getContentType()
        : jbdRedirectHeaders.get().get("content-type").get(0);
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
    return isJbd ? conn.getHeaderField(fieldName)
        : jbdRedirectHeaders.get().get(fieldName).get(0);
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
    return isJbd ? conn.getHeaderFields()
        : jbdRedirectHeaders.get();
  }

  @Override
  public long getIfModifiedSince() {
    return conn.getIfModifiedSince();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return isJbd ? StreamHandler.injectedStream(conn) : new ByteArrayInputStream(new byte[0]);
  }

  @Override
  public long getLastModified() {
    return conn.getLastModified();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return conn.getOutputStream();
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