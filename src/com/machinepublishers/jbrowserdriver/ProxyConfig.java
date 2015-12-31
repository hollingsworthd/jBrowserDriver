/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC
 * 
 * Sales and support: ops@machinepublishers.com
 * Updates: https://github.com/MachinePublishers/jBrowserDriver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.machinepublishers.jbrowserdriver;

import java.io.Serializable;

/**
 * Proxy server settings.
 */
public class ProxyConfig implements Serializable {
  private final Type type;
  private final String host;
  private final int port;
  private final String hostAndPort;
  private final String user;
  private final String password;
  private final boolean expectContinue;

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
    expectContinue = false;
  }

  /**
   * Creates a proxy.
   * 
   * @param type
   * @param host
   * @param port
   */
  public ProxyConfig(Type type, String host, int port) {
    this(type, host, port, null, null, true);
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
    this(type, host, port, user, password, true);
  }

  /**
   * Creates a proxy.
   * 
   * @param type
   * @param host
   * @param port
   * @param user
   * @param password
   * @param expectContinue
   *          Whether the proxy uses "Expect: 100-Continue" functionality
   */
  public ProxyConfig(Type type, String host, int port, String user, String password, boolean expectContinue) {
    this.type = type;
    this.host = host;
    this.port = port;
    this.hostAndPort = host + ":" + port;
    this.user = user;
    this.password = password;
    this.expectContinue = expectContinue;
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

  boolean expectContinue() {
    return expectContinue;
  }
}
