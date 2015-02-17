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

public class Proxy {
  private final Type type;
  private final String host;
  private final int port;
  private final String hostAndPort;
  private final String user;
  private final String password;

  public static enum Type {
    SOCKS, HTTP
  }

  /**
   * Creates a direct connection (no proxy).
   */
  public Proxy() {
    type = null;
    host = null;
    port = -1;
    hostAndPort = null;
    user = null;
    password = null;
  }

  public Proxy(Type type, String host, int port, String user, String password) {
    this.type = type;
    this.host = host;
    this.port = port;
    this.hostAndPort = host + ":" + port;
    this.user = user;
    this.password = password;
  }

  boolean directConnection() {
    return type == null || host == null || host.isEmpty() || port == -1;
  }

  boolean credentials() {
    return user != null && !user.isEmpty() && password != null;
  }

  java.net.Proxy.Type type() {
    return type == Type.SOCKS ? java.net.Proxy.Type.SOCKS : java.net.Proxy.Type.HTTP;
  }

  String host() {
    return host;
  }

  int port() {
    return port;
  }

  String hostAndPort() {
    return hostAndPort;
  }

  String user() {
    return user == null ? "" : user;
  }

  String password() {
    return password == null ? "" : password;
  }
}
