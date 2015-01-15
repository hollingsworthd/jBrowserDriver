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

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class Settings {
  private static final Map<Long, Settings> registry = new HashMap<Long, Settings>();
  private final Map<String, String> headers = new LinkedHashMap<String, String>();
  private static final Collection<String> defaultHeaders = new HashSet<String>(Arrays.asList(new String[] {
      "Accept", "User-Agent", "Accept-Encoding", "Accept-Language", "Accept-Charset"
  }));

  private ProxyType proxyType;
  private String proxyHost;
  private int proxyPort;
  private String proxyUser;
  private String proxyPassword;

  public Settings() {
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    headers.put("User-Agent",
        "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2114.2 Safari/537.36");
    headers.put("Accept-Encoding", "gzip,deflate");
    headers.put("Accept-Language", "en-US,en;q=0.5");
    headers.put("Accept-Charset", "");
  }

  public static enum ProxyType {
    SOCKS_5, SOCKS_4, HTTP, SSL, ALL
  };

  public void setProxy(ProxyType type, String host, int port, String user, String password) {
    this.proxyType = type;
    this.proxyHost = host;
    this.proxyPort = port;
    this.proxyUser = user;
    this.proxyPassword = password;
  }

  synchronized boolean hasProxy() {
    return proxyType != null
        && proxyHost != null && !proxyHost.isEmpty()
        && proxyPort > 0;
  }

  synchronized ProxyType proxyType() {
    return proxyType;
  }

  synchronized String proxyHost() {
    return proxyHost;
  }

  synchronized int proxyPort() {
    return proxyPort;
  }

  synchronized String proxyUser() {
    return proxyUser == null ? "" : proxyUser;
  }

  synchronized String proxyPassword() {
    return proxyPassword == null ? "" : proxyPassword;
  }

  public synchronized void setHeader(String name, String value) {
    headers.put(name, value);
  }

  synchronized Collection<String> headerNames() {
    return headers.keySet();
  }

  synchronized String header(String name) {
    return headers.get(name);
  }

  static synchronized void register(Long id, Settings settings) {
    registry.put(id, settings);
  }

  static synchronized void deregister(Long id) {
    registry.remove(id);
  }

  static synchronized Settings requestPropertyHelper(HttpURLConnection conn,
      Settings settings, boolean add, String name, String value) {
    if (name.equals("User-Agent")) {
      settings = registry.get(Long.parseLong(value));
      for (String curName : settings.headerNames()) {
        String curVal = settings.header(curName);
        if (curVal != null && !curVal.isEmpty()) {
          conn.setRequestProperty(curName, curVal);
        }
      }
    } else if (!defaultHeaders.contains(name)
        && (settings == null || !settings.headerNames().contains(name))) {
      if (add) {
        conn.addRequestProperty(name, value);
      } else {
        conn.setRequestProperty(name, value);
      }
    }
    return settings;
  }
}
