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

import java.util.HashMap;
import java.util.Map;

import com.machinepublishers.jbrowserdriver.config.BrowserProperties;
import com.machinepublishers.jbrowserdriver.config.BrowserTimeZone;
import com.machinepublishers.jbrowserdriver.config.Proxy;
import com.machinepublishers.jbrowserdriver.config.RequestHeaders;

public class Settings {
  private static final Map<Long, Settings> registry = new HashMap<Long, Settings>();
  private RequestHeaders requestHeaders = new RequestHeaders();
  private BrowserTimeZone browserTimeZone = BrowserTimeZone.UTC;
  private BrowserProperties browserProperties = new BrowserProperties();
  private Proxy proxy = new Proxy();

  public void setHeaders(RequestHeaders requestHeaders) {
    this.requestHeaders = requestHeaders;
  }

  public void setBrowserTimeZone(BrowserTimeZone browserTimeZone) {
    this.browserTimeZone = browserTimeZone;
  }

  public void setBrowserProperties(BrowserProperties browserProperties) {
    this.browserProperties = browserProperties;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  public RequestHeaders headers() {
    return requestHeaders;
  }

  public BrowserTimeZone browserTimeZone() {
    return browserTimeZone;
  }

  public BrowserProperties browserProperties() {
    return browserProperties;
  }

  public Proxy proxy() {
    return proxy;
  }

  static synchronized void register(Long id, Settings settings) {
    registry.put(id, settings);
  }

  static synchronized void deregister(Long id) {
    registry.remove(id);
  }

  public static synchronized Settings get(Long id) {
    return registry.get(id);
  }
}
