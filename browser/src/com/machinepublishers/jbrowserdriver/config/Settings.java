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

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Settings {
  static {
    if (!"true".equals(System.getProperty("browsergui"))) {
      System.setProperty("glass.platform", "Monocle");
      System.setProperty("monocle.platform", "Headless");
      System.setProperty("prism.order", "sw");
      //AWT gui is never used anyhow, but set this to prevent programmer errors
      System.setProperty("java.awt.headless", "true");
    }
    try {
      URL.setURLStreamHandlerFactory(new StreamHandler());
      ResponseCache.setDefault(null);
    } catch (Throwable t) {
      Field factory = null;
      try {
        factory = URL.class.getDeclaredField("factory");
        factory.setAccessible(true);
        Object curFac = factory.get(null);

        //assume we're in the Eclipse jar-in-jar loader
        Field chainedFactory = curFac.getClass().getDeclaredField("chainFac");
        chainedFactory.setAccessible(true);
        chainedFactory.set(curFac, new StreamHandler());
      } catch (Throwable t2) {
        try {
          //this should work regardless
          factory.set(null, new StreamHandler());
        } catch (Throwable t3) {}
      }
    }
    CookieHandler.setDefault(new CookieManager());
    ResponseCache.setDefault(new ResponseCache() {
      @Override
      public CacheRequest put(URI arg0, URLConnection arg1) throws IOException {
        return null;
      }

      @Override
      public CacheResponse get(URI arg0, String arg1, Map<String, List<String>> arg2) throws IOException {
        return null;
      }
    });
  }
  private static final Random rand = new Random();
  private final RequestHeaders requestHeaders;
  private final BrowserTimeZone browserTimeZone;
  private final BrowserProperties browserProperties;
  private final Proxy proxy;
  private static final AtomicLong settingsId = new AtomicLong();
  private final long mySettingsId;
  private final String script;

  public Settings() {
    this(new RequestHeaders(), BrowserTimeZone.UTC, new BrowserProperties(), new Proxy());
  }

  public Settings(RequestHeaders requestHeaders, BrowserTimeZone browserTimeZone,
      BrowserProperties browserProperties, Proxy proxy) {
    mySettingsId = settingsId.incrementAndGet();
    this.requestHeaders = requestHeaders;
    this.browserTimeZone = browserTimeZone;
    this.browserProperties = browserProperties;
    this.proxy = proxy;

    StringBuilder scriptBuilder = new StringBuilder();
    String scriptId = "A" + rand.nextLong();
    scriptBuilder.append("<script id='" + scriptId + "' language='javascript'>");
    scriptBuilder.append("try{");
    scriptBuilder.append(browserTimeZone().script());
    scriptBuilder.append(browserProperties().script());
    scriptBuilder.append("}catch(e){}");
    scriptBuilder.append("document.getElementsByTagName('head')[0].removeChild("
        + "document.getElementById('" + scriptId + "'));");
    scriptBuilder.append("</script>");
    script = scriptBuilder.toString();
  }

  long id() {
    return mySettingsId;
  }

  RequestHeaders headers() {
    return requestHeaders;
  }

  BrowserTimeZone browserTimeZone() {
    return browserTimeZone;
  }

  BrowserProperties browserProperties() {
    return browserProperties;
  }

  Proxy proxy() {
    return proxy;
  }

  String script() {
    return script;
  }
}
