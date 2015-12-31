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
