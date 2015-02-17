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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import sun.net.www.protocol.https.HttpsURLConnectionImpl;

class StreamHandler implements URLStreamHandlerFactory {
  private static final HttpHandler httpHandler = new HttpHandler();
  private static final HttpsHandler httpsHandler = new HttpsHandler();

  private static final Set<String> adHosts = new HashSet<String>();
  public static final URL BLOCKED_URL;
  static {
    URL blockedUrlTmp = null;
    try {
      File tmpFile = Files.createTempFile("jbd-null-file", ".txt").toFile();
      tmpFile.deleteOnExit();
      blockedUrlTmp = new URL("file:///" + tmpFile.getCanonicalPath());
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(StreamHandler.class.getResourceAsStream("./ad-hosts.txt")));
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        adHosts.add(line.trim().toLowerCase());
      }
    } catch (Throwable t) {
      Logs.exception(t);
    }
    BLOCKED_URL = blockedUrlTmp;
  }

  StreamHandler() {}

  static class HttpHandler extends sun.net.www.protocol.http.Handler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      url = isBlocked(url.getHost()) ? BLOCKED_URL : url;
      StreamConnection conn =
          new StreamConnection((sun.net.www.protocol.http.HttpURLConnection) super.openConnection(url));
      return conn;
    }

    private URLConnection defaultConnection(URL url) throws IOException {
      return super.openConnection(url);
    }
  }

  static class HttpsHandler extends sun.net.www.protocol.https.Handler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      url = isBlocked(url.getHost()) ? BLOCKED_URL : url;
      StreamConnection conn =
          new StreamConnection((HttpsURLConnectionImpl) super.openConnection(url));
      return conn;
    }

    private URLConnection defaultConnection(URL url) throws IOException {
      return super.openConnection(url);
    }
  }

  private static boolean isBlocked(String urlHost) {
    String[] parts = urlHost.toLowerCase().split("\\.");
    for (int i = parts.length - 2; i > -1; --i) {
      StringBuilder builder = new StringBuilder();
      for (int j = i; j < parts.length; j++) {
        builder.append(parts[j]);
        if (j + 1 < parts.length) {
          builder.append(".");
        }
      }
      if (adHosts.contains(builder.toString())) {
        if (Logs.TRACE) {
          System.out.println("Ad blocked: " + urlHost);
        }
        return true;
      }
    }
    return false;
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

  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {
    if ("https".equals(protocol)) {
      return httpsHandler;
    }
    if ("http".equals(protocol)) {
      return httpHandler;
    }
    if ("about".equals(protocol)) {
      return new com.sun.webkit.network.about.Handler();
    }
    if ("data".equals(protocol)) {
      return new com.sun.webkit.network.data.Handler();
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
