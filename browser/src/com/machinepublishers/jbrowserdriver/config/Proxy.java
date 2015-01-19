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

public class Proxy {
  private final Type type;
  private final String host;
  private final int port;
  private final String user;
  private final String password;

  public static enum Type {
    SOCKS_5, SOCKS_4, HTTP, SSL, ALL
  };

  /**
   * Creates a direct connection (no proxy).
   */
  public Proxy() {
    type = null;
    host = null;
    port = -1;
    user = null;
    password = null;
  }

  public Proxy(Type type, String host, int port, String user, String password) {
    this.type = type;
    this.host = host;
    this.port = port;
    this.user = user;
    this.password = password;
  }

  synchronized boolean hasProxy() {
    return type != null
        && host != null && !host.isEmpty()
        && port > 0;
  }

  synchronized Type proxyType() {
    return type;
  }

  synchronized String proxyHost() {
    return host;
  }

  synchronized int proxyPort() {
    return port;
  }

  synchronized String proxyUser() {
    return user == null ? "" : user;
  }

  synchronized String proxyPassword() {
    return password == null ? "" : password;
  }
}
