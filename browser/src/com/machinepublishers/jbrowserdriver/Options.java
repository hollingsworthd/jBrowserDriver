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

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver.ImeHandler;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.logging.Logs;

class Options implements org.openqa.selenium.WebDriver.Options {
  private final ImeHandler imeHandler = new com.machinepublishers.jbrowserdriver.ImeHandler();
  private final Logs logs = com.machinepublishers.jbrowserdriver.Logs.instance();
  private final AtomicReference<com.machinepublishers.jbrowserdriver.Window> window;
  private final CookieManager cookieManager;
  private final AtomicReference<com.machinepublishers.jbrowserdriver.Timeouts> timeouts;

  Options(final AtomicReference<com.machinepublishers.jbrowserdriver.Window> window,
      final CookieManager cookieManager,
      final AtomicReference<com.machinepublishers.jbrowserdriver.Timeouts> timeouts) {
    this.window = window;
    this.cookieManager = cookieManager;
    this.timeouts = timeouts;
  }

  private static HttpCookie convert(Cookie in) {
    HttpCookie out = new HttpCookie(in.getName(), in.getValue());
    out.setDomain(in.getDomain());
    out.setHttpOnly(in.isHttpOnly());
    if (in.getExpiry() != null) {
      out.setMaxAge(Math.max(0, (in.getExpiry().getTime() - System.currentTimeMillis()) / 1000));
    }
    out.setPath(in.getPath());
    out.setSecure(in.isSecure());
    out.setValue(in.getValue());
    out.setVersion(1);
    return out;
  }

  private static Cookie convert(HttpCookie in) {
    Date expiry = in.getMaxAge() < 0 ? null : new Date((in.getMaxAge() * 1000) + System.currentTimeMillis());
    return new Cookie(in.getName(),
        in.getValue(),
        in.getDomain(),
        in.getPath(),
        expiry,
        in.getSecure(),
        in.isHttpOnly());
  }

  @Override
  public void addCookie(Cookie cookie) {
    cookieManager.getCookieStore().add(null, convert(cookie));
  }

  @Override
  public void deleteAllCookies() {
    cookieManager.getCookieStore().removeAll();
  }

  @Override
  public void deleteCookie(Cookie cookie) {
    List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
    String toDelete = cookie.getDomain().toLowerCase()
        + "\n" + cookie.getName().toLowerCase()
        + "\n" + cookie.getPath().toLowerCase();
    for (HttpCookie cur : cookies) {
      String curString = cur.getDomain().toLowerCase()
          + "\n" + cur.getName().toLowerCase()
          + "\n" + cur.getPath().toLowerCase();
      if (toDelete.equals(curString)) {
        if (!cookieManager.getCookieStore().remove(null, cur)) {
          for (URI uri : cookieManager.getCookieStore().getURIs()) {
            if (cookieManager.getCookieStore().remove(uri, cur)) {
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public void deleteCookieNamed(String name) {
    for (HttpCookie cur : cookieManager.getCookieStore().getCookies()) {
      if (cur.getName().equals(name)) {
        cookieManager.getCookieStore().remove(null, cur);
        for (URI uri : cookieManager.getCookieStore().getURIs()) {
          cookieManager.getCookieStore().remove(uri, cur);
        }
      }
    }
  }

  @Override
  public Cookie getCookieNamed(String name) {
    for (HttpCookie cur : cookieManager.getCookieStore().getCookies()) {
      if (cur.getName().equals(name)) {
        return convert(cur);
      }
    }
    return null;
  }

  @Override
  public Set<Cookie> getCookies() {
    Set<Cookie> cookies = new LinkedHashSet<Cookie>();
    for (HttpCookie cur : cookieManager.getCookieStore().getCookies()) {
      cookies.add(convert(cur));
    }
    return cookies;
  }

  @Override
  public ImeHandler ime() {
    return imeHandler;
  }

  @Override
  public Logs logs() {
    return logs;
  }

  @Override
  public Timeouts timeouts() {
    return timeouts.get();
  }

  @Override
  public Window window() {
    return window.get();
  }
}
