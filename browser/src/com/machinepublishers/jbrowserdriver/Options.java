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

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver.ImeHandler;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.logging.Logs;

public class Options implements org.openqa.selenium.WebDriver.Options {
  private final CookieManager cookieManager;
  private final ImeHandler imeHandler = new com.machinepublishers.jbrowserdriver.ImeHandler();
  private final Logs logs = com.machinepublishers.jbrowserdriver.Logs.instance();
  private final Window window;
  private final Timeouts timeouts;

  public Options(Window window, Timeouts timeouts, CookieManager cookieManager) {
    this.window = window;
    this.timeouts = timeouts;
    this.cookieManager = cookieManager;
  }

  private static HttpCookie convert(Cookie in) {
    HttpCookie out = new HttpCookie(in.getName(), in.getValue());
    out.setDomain(in.getDomain());
    out.setHttpOnly(in.isHttpOnly());
    out.setMaxAge(in.getExpiry().getTime() - System.currentTimeMillis());
    out.setPath(in.getPath());
    out.setSecure(in.isSecure());
    out.setValue(in.getValue());
    out.setVersion(1);
    return out;
  }

  private static Cookie convert(HttpCookie in) {
    return new Cookie(in.getName(),
        in.getValue(),
        in.getDomain(),
        in.getPath(),
        new Date(in.getMaxAge() + System.currentTimeMillis()),
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
    return timeouts;
  }

  @Override
  public Window window() {
    return window;
  }
}
