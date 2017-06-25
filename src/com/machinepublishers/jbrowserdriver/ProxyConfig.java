/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
 * https://github.com/MachinePublishers/jBrowserDriver
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
import java.util.Collections;
import java.util.Set;

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
  private final Set<String> nonProxyHosts;

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
    nonProxyHosts = Collections.emptySet();
  }

  /**
   * Creates a proxy.
   * 
   * @param type
   * @param host
   * @param port
   */
  public ProxyConfig(Type type, String host, int port) {
    this(type, host, port, null, null, true, Collections.emptySet());
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
    this(type, host, port, user, password, true, Collections.emptySet());
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
    this(type, host, port, user, password, expectContinue, Collections.emptySet());
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
   * @param nonProxyHosts
   *          Set of hosts that should be reached directly, bypassing the proxy.
   */
  public ProxyConfig(Type type, String host, int port, String user, String password, boolean expectContinue, Set<String> nonProxyHosts) {
    this.type = type;
    this.host = host;
    this.port = port;
    this.hostAndPort = host + ":" + port;
    this.user = user;
    this.password = password;
    this.expectContinue = expectContinue;
    this.nonProxyHosts = nonProxyHosts;
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

  Set<String> nonProxyHosts() {
    return nonProxyHosts;
  }
}
