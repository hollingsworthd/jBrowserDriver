/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

/**
 * Proxy server settings.
 */
public class ProxyConfig {
  private final Type type;
  private final String host;
  private final int port;
  private final String hostAndPort;
  private final String user;
  private final String password;

  /**
   * The proxy type.
   */
  public static enum Type {
    /**
     * SOCKS proxy.
     */
    SOCKS,
    /**
     * HTTP/HTTPS proxy.
     */
    HTTP
  }

  /**
   * Creates a direct connection (no proxy).
   */
  public ProxyConfig() {
    type = null;
    host = null;
    port = -1;
    hostAndPort = null;
    user = null;
    password = null;
  }

  /**
   * Creates a proxy.
   * 
   * @param type
   * @param host
   * @param port
   * @param user
   * @param password
   */
  public ProxyConfig(Type type, String host, int port, String user, String password) {
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

  Type type() {
    return type;
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
