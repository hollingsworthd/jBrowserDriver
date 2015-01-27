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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import sun.net.www.protocol.https.HttpsURLConnectionImpl;

import com.machinepublishers.jbrowserdriver.Logs;
import com.machinepublishers.jbrowserdriver.Util;

class StreamHandler implements URLStreamHandlerFactory {
  private static final Pattern jbdProtocol = Pattern.compile("^jbds?[0-9]+://");
  private static final HttpHandler httpHandler = new HttpHandler();
  private static final HttpsHandler httpsHandler = new HttpsHandler();
  private static int monitors;
  private static final Object lock = new Object();
  private static final Map<String, StreamConnection> connections = new HashMap<String, StreamConnection>();

  StreamHandler() {}

  static class HttpHandler extends sun.net.www.protocol.http.Handler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      URL newUrl = new URL(jbdProtocol.matcher(url.toExternalForm()).replaceFirst("http://"));
      StreamConnection conn =
          new StreamConnection((sun.net.www.protocol.http.HttpURLConnection) super.openConnection(
              newUrl), !newUrl.toExternalForm().equals(url.toExternalForm()));
      synchronized (lock) {
        connections.put(url.toExternalForm(), conn);
      }
      return conn;
    }

    private URLConnection defaultConnection(URL url) throws IOException {
      return super.openConnection(url);
    }
  }

  static class HttpsHandler extends sun.net.www.protocol.https.Handler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      URL newUrl = new URL(jbdProtocol.matcher(url.toExternalForm()).replaceFirst("https://"));
      StreamConnection conn =
          new StreamConnection((HttpsURLConnectionImpl) super.openConnection(
              newUrl), !newUrl.toExternalForm().equals(url.toExternalForm()));
      synchronized (lock) {
        connections.put(url.toExternalForm(), conn);
      }
      return conn;
    }

    private URLConnection defaultConnection(URL url) throws IOException {
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
        for (StreamConnection conn : connections.values()) {
          Util.close(conn);
        }
        connections.clear();
        SettingsManager.clearConnections();
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
    if (protocol.startsWith("jbds") || "https".equals(protocol)) {
      return httpsHandler;
    }
    if (protocol.startsWith("jbd") || "http".equals(protocol)) {
      return httpHandler;
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

}
