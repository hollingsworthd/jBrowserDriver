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

import java.util.HashMap;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;

class ProxyAuth implements CredentialsProvider {
  private static final ProxyAuth instance = new ProxyAuth();
  private static final Map<String, Credentials> proxies = new HashMap<String, Credentials>();
  private static final Object lock = new Object();

  private ProxyAuth() {}

  static ProxyAuth instance() {
    return instance;
  }

  static void add(final ProxyConfig proxy) {
    if (proxy != null && !proxy.directConnection() && proxy.credentials()) {
      synchronized (lock) {
        proxies.put(proxy.hostAndPort(),
            new UsernamePasswordCredentials(proxy.user(), proxy.password()));
      }
    }
  }

  static void remove(final ProxyConfig proxy) {
    if (proxy != null && !proxy.directConnection() && proxy.credentials()) {
      synchronized (lock) {
        proxies.remove(proxy.hostAndPort());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Credentials getCredentials(AuthScope authScope) {
    synchronized (lock) {
      return proxies.get(new StringBuilder()
          .append(authScope.getHost()).append(":").append(authScope.getPort()).toString());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCredentials(AuthScope authscope, Credentials credentials) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
