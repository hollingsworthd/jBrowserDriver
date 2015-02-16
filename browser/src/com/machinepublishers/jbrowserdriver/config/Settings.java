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

import java.lang.reflect.Field;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.machinepublishers.jbrowserdriver.Logs;
import com.machinepublishers.jbrowserdriver.Util;
import com.machinepublishers.jbrowserdriver.config.StreamInjectors.Injector;
import com.sun.webkit.network.CookieManager;

public class Settings {
  private static final boolean headless;
  static {
    if (!"true".equals(System.getProperty("jbd.browsergui"))) {
      headless = true;
      System.setProperty("glass.platform", "Monocle");
      System.setProperty("monocle.platform", "Headless");
      System.setProperty("prism.order", "sw");
      //AWT gui is never used anyhow, but set this to prevent programmer errors
      System.setProperty("java.awt.headless", "true");
    } else {
      headless = false;
    }
    try {
      URL.setURLStreamHandlerFactory(new StreamHandler());
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
    final Pattern head = Pattern.compile("<head\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Pattern html = Pattern.compile("<html\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Pattern body = Pattern.compile("<body\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    StreamInjectors.add(new Injector() {
      @Override
      public byte[] inject(HttpURLConnection connection, String originalUrl, byte[] inflatedContent) {
        try {
          AtomicReference<Settings> settings;
          settings = SettingsManager.get(connection);
          if (!"false".equals(System.getProperty("jbd.quickrender"))
              && connection.getContentType() != null
              && (connection.getContentType().startsWith("image/")
                  || connection.getContentType().startsWith("video/")
                  || connection.getContentType().startsWith("audio/")
                  || connection.getContentType().startsWith("model/"))) {
            if (Logs.TRACE) {
              System.out.println("Media discarded: " + originalUrl);
            }
            return new byte[0];
          } else if (connection.getContentType() != null
              && connection.getContentType().indexOf("text/html") > -1
              && StreamHandler.isPrimaryDocument(originalUrl)) {
            String injected = null;
            String charset = Util.charset(connection);
            String content = new String(inflatedContent, charset);
            Matcher matcher = head.matcher(content);
            if (matcher.find()) {
              injected = matcher.replaceFirst(matcher.group(0) + settings.get().script());
            } else {
              matcher = html.matcher(content);
              if (matcher.find()) {
                injected = matcher.replaceFirst(
                    matcher.group(0) + "<head>" + settings.get().script() + "</head>");
              } else {
                matcher = body.matcher(content);
                if (matcher.find()) {
                  injected = ("<html><head>" + settings.get().script() + "</head>"
                      + content + "</html>");
                }
              }
            }
            return injected == null ? null : injected.getBytes(charset);
          }
        } catch (Throwable t) {}
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

  /**
   * Create default settings.
   */
  public Settings() {
    this(null, null, null, null);
  }

  /**
   * Pass null for any parameter which you want left as default.
   */
  public Settings(final RequestHeaders requestHeaders, final BrowserTimeZone browserTimeZone,
      final BrowserProperties browserProperties, final Proxy proxy) {
    mySettingsId = settingsId.incrementAndGet();
    this.requestHeaders = requestHeaders == null ? new RequestHeaders() : requestHeaders;
    this.browserTimeZone = browserTimeZone == null ? BrowserTimeZone.UTC : browserTimeZone;
    this.browserProperties = browserProperties == null ? new BrowserProperties() : browserProperties;
    this.proxy = proxy == null ? new Proxy() : proxy;

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

  static boolean headless() {
    return headless;
  }
}
