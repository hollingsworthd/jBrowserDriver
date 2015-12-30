/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
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

import java.util.LinkedHashSet;
import java.util.Set;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver.ImeHandler;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.logging.Logs;

class OptionsLocal implements org.openqa.selenium.WebDriver.Options {
  private final Set<Cookie> cookies;
  private final Logs logs;

  OptionsLocal(Set<Cookie> cookies, Logs logs) {
    this.cookies = cookies;
    this.logs = logs;
  }

  @Override
  public void addCookie(Cookie cookie) {
    cookies.add(cookie);
  }

  @Override
  public void deleteCookieNamed(String name) {
    Cookie toRemove = null;
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals(name)) {
        toRemove = cookie;
        break;
      }
    }
    if (toRemove != null) {
      cookies.remove(toRemove);
    }
  }

  @Override
  public void deleteCookie(Cookie cookie) {
    cookies.remove(cookie);

  }

  @Override
  public void deleteAllCookies() {
    cookies.clear();
  }

  @Override
  public Set<Cookie> getCookies() {
    return new LinkedHashSet<Cookie>(cookies);
  }

  @Override
  public Cookie getCookieNamed(String name) {
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals(name)) {
        return cookie;
      }
    }
    return null;
  }

  @Override
  public org.openqa.selenium.WebDriver.Timeouts timeouts() {
    return null;
  }

  @Override
  public ImeHandler ime() {
    return null;
  }

  @Override
  public Window window() {
    return null;
  }

  @Override
  public Logs logs() {
    return logs;
  }
}
