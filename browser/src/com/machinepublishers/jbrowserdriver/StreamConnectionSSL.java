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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.cert.X509Certificate;

import sun.net.www.protocol.https.HttpsURLConnectionImpl;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class StreamConnectionSSL extends HttpsURLConnectionImpl {
  private static final Object lock = new Object();
  private static SSLSocketFactory socketFactory;
  private static long lastCertUpdate;
  //a good pem source: https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt
  private static final String pemFile = System.getProperty("pemfile");
  private final HttpsURLConnectionImpl conn;
  private Settings settings;
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

  public static SSLSocketFactory updatedSocketFactory() {
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
            pemBlocks = StreamHandler.toString(remotePemFile.getInputStream(), StreamHandler.charset(remotePemFile));
            cachedPemFile.delete();
            Files.write(Paths.get(cachedPemFile.getAbsolutePath()), pemBlocks.getBytes("utf-8"));
          } else {
            pemBlocks = new String(Files.readAllBytes(Paths.get(new File(location).getAbsolutePath())), "utf-8");
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

  public StreamConnectionSSL(HttpsURLConnectionImpl conn) throws IOException {
    super(dummy);
    this.conn = conn;
    SSLSocketFactory socketFactory = updatedSocketFactory();
    if (socketFactory != null) {
      this.conn.setSSLSocketFactory(socketFactory);
    }
  }

  @Override
  public void setRequestProperty(String arg0, String arg1) {
    settings = Settings.requestPropertyHelper(conn, settings, false, arg0, arg1);
  }

  @Override
  public void addRequestProperty(String arg0, String arg1) {
    settings = Settings.requestPropertyHelper(conn, settings, true, arg0, arg1);
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
  protected void finalize() throws Throwable {
    try {
      super.finalize();
    } catch (Throwable t) {}
    try {
      Method method = conn.getClass().getDeclaredMethod("finalize");
      method.setAccessible(true);
      method.invoke(conn);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    StreamHandler.clearFields(this);
    StreamHandler.clearFields(conn);
  }

  @Override
  public X509Certificate[] getServerCertificateChain() {
    return conn.getServerCertificateChain();
  }

  @Override
  protected boolean isConnected() {
    try {
      Method method = conn.getClass().getDeclaredMethod("isConnected");
      method.setAccessible(true);
      return (Boolean) method.invoke(conn);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    return false;
  }

  @Override
  protected void setConnected(boolean arg0) {
    try {
      Method method = conn.getClass().getDeclaredMethod("setConnected", boolean.class);
      method.setAccessible(true);
      method.invoke(conn, arg0);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  @Override
  protected void setNewClient(URL arg0, boolean arg1) throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("setNewClient", URL.class, boolean.class);
      method.setAccessible(true);
      method.invoke(conn, arg0, arg1);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  @Override
  protected void setNewClient(URL arg0) throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("setNewClient", URL.class);
      method.setAccessible(true);
      method.invoke(conn, arg0);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  @Override
  protected void setProxiedClient(URL arg0, String arg1, int arg2, boolean arg3) throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("setProxiedClient",
          URL.class, String.class, int.class, boolean.class);
      method.setAccessible(true);
      method.invoke(conn, arg0, arg1, arg2, arg3);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  @Override
  protected void setProxiedClient(URL arg0, String arg1, int arg2) throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("setProxiedClient",
          URL.class, String.class, int.class);
      method.setAccessible(true);
      method.invoke(conn, arg0, arg1, arg2);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  @Override
  public String getCipherSuite() {
    return conn.getCipherSuite();
  }

  @Override
  public HostnameVerifier getHostnameVerifier() {
    return conn.getHostnameVerifier();
  }

  @Override
  public Certificate[] getLocalCertificates() {
    return conn.getLocalCertificates();
  }

  @Override
  public Principal getLocalPrincipal() {
    return conn.getLocalPrincipal();
  }

  @Override
  public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
    return conn.getPeerPrincipal();
  }

  @Override
  public SSLSocketFactory getSSLSocketFactory() {
    return conn.getSSLSocketFactory();
  }

  @Override
  public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
    return conn.getServerCertificates();
  }

  @Override
  public void setHostnameVerifier(HostnameVerifier arg0) {
    conn.setHostnameVerifier(arg0);
  }

  @Override
  public void setSSLSocketFactory(SSLSocketFactory arg0) {
    conn.setSSLSocketFactory(arg0);
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
    return conn.getResponseCode();
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
    conn.connect();
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
    return conn.getContentEncoding();
  }

  @Override
  public int getContentLength() {
    return conn.getContentLength();
  }

  @Override
  public long getContentLengthLong() {
    return conn.getContentLengthLong();
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
  public String getHeaderField(String arg0) {
    return conn.getHeaderField(arg0);
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
  public InputStream getInputStream() throws IOException {
    return StreamHandler.injectedStream(conn);
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