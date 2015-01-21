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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import sun.net.www.protocol.https.HttpsURLConnectionImpl;

import com.machinepublishers.jbrowserdriver.Logs;

class StreamHandler implements URLStreamHandlerFactory {
  private static final HttpHandler httpHandler = new HttpHandler();
  private static final HttpsHandler httpsHandler = new HttpsHandler();
  private static int monitors;
  private static final Object lock = new Object();
  private static final Map<String, HttpURLConnection> connections = new HashMap<String, HttpURLConnection>();
  private static final Pattern charsetPattern = Pattern.compile(
      "charset\\s*=\\s*([^;]+)", Pattern.CASE_INSENSITIVE);
  private static final Object injectorLock = new Object();
  private static final List<Injector> injectors = new ArrayList<Injector>();

  static void addInjector(Injector injector) {
    synchronized (injectorLock) {
      injectors.add(injector);
    }
  }

  static void removeInjector(Injector injector) {
    synchronized (injectorLock) {
      injectors.remove(injector);
    }
  }

  static void removeAllInjectors() {
    synchronized (injectorLock) {
      injectors.clear();
    }
  }

  static interface Injector {
    byte[] inject(HttpURLConnection connection, byte[] inflatedContent);
  }

  StreamHandler() {}

  static void clearFields(Object obj) throws Throwable {
    Class<?> cur = obj.getClass();
    while (!cur.equals(Object.class)) {
      Field[] fields = cur.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        try {
          if (!Modifier.isStatic(fields[i].getModifiers())
              && fields[i].getType() instanceof Object) {
            fields[i].setAccessible(true);
            fields[i].set(obj, null);
          }
        } catch (Throwable t) {}
      }
      cur = cur.getSuperclass();
    }
  }

  static class HttpHandler extends sun.net.www.protocol.http.Handler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      sun.net.www.protocol.http.HttpURLConnection conn =
          new StreamConnection((sun.net.www.protocol.http.HttpURLConnection) super.openConnection(url));
      conn.setDefaultUseCaches(false);
      conn.setUseCaches(false);
      synchronized (lock) {
        connections.put(url.toExternalForm(), conn);
      }
      return conn;
    }

    public URLConnection defaultConnection(URL url) throws IOException {
      return super.openConnection(url);
    }
  }

  static class HttpsHandler extends sun.net.www.protocol.https.Handler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      HttpsURLConnectionImpl conn =
          new StreamConnectionSSL((HttpsURLConnectionImpl) super.openConnection(url));
      conn.setDefaultUseCaches(false);
      conn.setUseCaches(false);
      synchronized (lock) {
        connections.put(url.toExternalForm(), conn);
      }
      return conn;
    }

    public URLConnection defaultConnection(URL url) throws IOException {
      return super.openConnection(url);
    }
  }

  static HttpURLConnection defaultConnection(String location) throws IOException {
    URL url = new URL(location);
    if (url.getProtocol().equalsIgnoreCase("http")) {
      return (HttpURLConnection) new HttpHandler().defaultConnection(url);
    }
    if (url.getProtocol().equalsIgnoreCase("https")) {
      return (HttpURLConnection) new HttpsHandler().defaultConnection(url);
    }
    return null;
  }

  public static void startStatusMonitor() {
    synchronized (lock) {
      if (monitors == 0) {
        for (HttpURLConnection conn : connections.values()) {
          try {
            conn.disconnect();
          } catch (Throwable t) {}
          try {
            conn.getErrorStream().close();
          } catch (Throwable t) {}
          try {
            conn.getInputStream().close();
          } catch (Throwable t) {}
          try {
            conn.getOutputStream().close();
          } catch (Throwable t) {}
        }
        connections.clear();
        System.gc();
        System.runFinalization();
        System.gc();
      }
      ++monitors;
    }
  }

  public static int stopStatusMonitor(String url) {
    synchronized (lock) {
      --monitors;
      int code = 0;
      try {
        if (connections.containsKey(url)) {
          code = connections.get(url).getResponseCode();
        }
      } catch (Throwable t) {
        Logs.exception(t);
      }
      return code;
    }
  }

  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {
    if ("http".equals(protocol)) {
      return httpHandler;
    }
    if ("https".equals(protocol)) {
      return httpsHandler;
    }
    if ("file".equals(protocol)) {
      return new sun.net.www.protocol.file.Handler();
    }
    if ("ftp".equals(protocol)) {
      return new sun.net.www.protocol.ftp.Handler();
    }
    if ("jar".equals(protocol)) {
      return new sun.net.www.protocol.jar.Handler();
    }
    if ("mailto".equals(protocol)) {
      return new sun.net.www.protocol.mailto.Handler();
    }
    if ("netdoc".equals(protocol)) {
      return new sun.net.www.protocol.netdoc.Handler();
    }
    return null;
  }

  static String toString(InputStream inputStream, String charset) {
    try {
      final char[] chars = new char[8192];
      StringBuilder builder = new StringBuilder();
      InputStreamReader reader = new InputStreamReader(inputStream, charset);
      for (int len; -1 != (len = reader.read(chars));) {
        builder.append(chars, 0, len);
      }
      return builder.toString();
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    }
  }

  static byte[] toBytes(InputStream inputStream) throws IOException {
    final byte[] bytes = new byte[8192];
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int len = 0; -1 != (len = inputStream.read(bytes));) {
      out.write(bytes, 0, len);
    }
    return out.toByteArray();
  }

  static String charset(URLConnection conn) {
    String charset = conn.getContentType();
    if (charset != null) {
      Matcher matcher = charsetPattern.matcher(charset);
      if (matcher.find()) {
        charset = matcher.group(1);
        if (Charset.isSupported(charset)) {
          return charset;
        }
      }
    }
    return "utf-8";
  }

  static InputStream injectedStream(HttpURLConnection conn) throws IOException {
    if (conn.getErrorStream() != null) {
      return conn.getInputStream();
    }
    byte[] connBytes = toBytes(conn.getInputStream());
    try {
      String encoding = conn.getContentEncoding();
      InputStream in = new ByteArrayInputStream(connBytes);
      if ("gzip".equalsIgnoreCase(encoding)) {
        in = new GZIPInputStream(in);
      } else if ("deflate".equalsIgnoreCase(encoding)) {
        in = new InflaterInputStream(in);
      }
      byte[] content = toBytes(in);
      synchronized (injectorLock) {
        for (Injector injector : injectors) {
          byte[] newContent = injector.inject(conn, content);
          if (newContent != null) {
            content = newContent;
          }
        }
      }
      if (content != null) {
        if ("gzip".equalsIgnoreCase(encoding)) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          GZIPOutputStream gzip = new GZIPOutputStream(out);
          gzip.write(content);
          gzip.close();
          return new ByteArrayInputStream(out.toByteArray());
        }
        if ("deflate".equalsIgnoreCase(encoding)) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          DeflaterOutputStream deflate = new DeflaterOutputStream(out);
          deflate.write(content);
          deflate.close();
          return new ByteArrayInputStream(out.toByteArray());
        }
        return new ByteArrayInputStream(content);
      }
    } catch (Throwable t) {
      Logs.exception(t);
    }
    return new ByteArrayInputStream(connBytes);
  }
}
