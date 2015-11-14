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

  @Override
  public Credentials getCredentials(AuthScope authScope) {
    synchronized (lock) {
      return proxies.get(authScope.getHost() + ":" + authScope.getPort());
    }
  }

  @Override
  public void setCredentials(AuthScope authscope, Credentials credentials) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }
}
