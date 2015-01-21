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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

import sun.net.www.http.HttpClient;
import sun.net.www.protocol.http.HttpURLConnection;

import com.machinepublishers.jbrowserdriver.Logs;

class StreamConnection extends HttpURLConnection {
  private final HttpURLConnection conn;
  private Settings settings;
  private static final URL dummy;
  static {
    URL dummyTmp = null;
    try {
      URI.create("about:blank").toURL();
    } catch (Throwable t) {}
    dummy = dummyTmp;
  }

  public StreamConnection(HttpURLConnection conn) {
    super(dummy, (Proxy) null);
    this.conn = conn;
  }

  @Override
  public void setRequestProperty(String arg0, String arg1) {
    settings = RequestHeaders.requestPropertyHelper(conn, settings, false, arg0, arg1);
  }

  @Override
  public void addRequestProperty(String arg0, String arg1) {
    settings = RequestHeaders.requestPropertyHelper(conn, settings, true, arg0, arg1);
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
    super.finalize();
    StreamHandler.clearFields(this);
    StreamHandler.clearFields(conn);
  }

  @Override
  public Object authObj() {
    return conn.authObj();
  }

  @Override
  public void authObj(Object arg0) {
    conn.authObj(arg0);
  }

  @Override
  public synchronized void doTunneling() throws IOException {
    conn.doTunneling();
  }

  @Override
  public CookieHandler getCookieHandler() {
    return conn.getCookieHandler();
  }

  @Override
  protected HttpClient getNewHttpClient(URL arg0, Proxy arg1, int arg2, boolean arg3) throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("getNewHttpClient",
          URL.class, Proxy.class, int.class, boolean.class);
      method.setAccessible(true);
      return (HttpClient) method.invoke(conn, arg0, arg1, arg2, arg3);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    return null;
  }

  @Override
  protected HttpClient getNewHttpClient(URL arg0, Proxy arg1, int arg2) throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("getNewHttpClient",
          URL.class, Proxy.class, int.class);
      method.setAccessible(true);
      return (HttpClient) method.invoke(conn, arg0, arg1, arg2);
    } catch (Throwable t) {
      Logs.exception(t);
    }
    return null;
  }

  @Override
  protected void plainConnect() throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("plainConnect");
      method.setAccessible(true);
      method.invoke(conn);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  @Override
  protected void plainConnect0() throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("plainConnect0");
      method.setAccessible(true);
      method.invoke(conn);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  @Override
  protected void proxiedConnect(URL arg0, String arg1, int arg2, boolean arg3) throws IOException {
    try {
      Method method = conn.getClass().getDeclaredMethod("proxiedConnect",
          URL.class, String.class, int.class, boolean.class);
      method.setAccessible(true);
      method.invoke(conn, arg0, arg1, arg2, arg3);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  @Override
  public void setAuthenticationProperty(String arg0, String arg1) {
    conn.setAuthenticationProperty(arg0, arg1);
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
          URL.class, String.class, Integer.class, boolean.class);
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
  public void setTunnelState(TunnelState arg0) {
    conn.setTunnelState(arg0);
  }

  @Override
  public boolean streaming() {
    return conn.streaming();
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