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

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.openqa.selenium.Cookie;

import com.sun.webkit.network.CookieManager;

class OptionsServer extends RemoteObject implements OptionsRemote,
    org.openqa.selenium.WebDriver.Options {
  private static final CookieStore cookieStore = (CookieStore) CookieManager.getDefault();
  private final Context context;
  private final ImeHandlerServer imeHandler = new com.machinepublishers.jbrowserdriver.ImeHandlerServer();
  private final AtomicReference<com.machinepublishers.jbrowserdriver.TimeoutsServer> timeouts;
  private static final Pattern domain = Pattern.compile(".*?://(?:[^/]*@)?\\[?([^\\]:/]*).*");

  OptionsServer(final Context context,
      final AtomicReference<com.machinepublishers.jbrowserdriver.TimeoutsServer> timeouts)
      throws RemoteException {
    this.context = context;
    this.timeouts = timeouts;
  }

  private org.apache.http.cookie.Cookie convert(Cookie in) {
    BasicClientCookie out = new BasicClientCookie(in.getName(), in.getValue());
    String domainStr = null;
    if (StringUtils.isEmpty(in.getDomain())) {
      String urlStr = context.item().engine.get().getLocation();
      try {
        URL url = new URL(urlStr);
        domainStr = url.getHost();
      } catch (MalformedURLException e) {
        Matcher matcher = domain.matcher(urlStr);
        if (matcher.matches()) {
          domainStr = matcher.group(1);
        }
      }
    }
    out.setDomain(domainStr == null ? in.getDomain() : domainStr);
    if (in.getExpiry() != null) {
      out.setExpiryDate(in.getExpiry());
    }
    out.setPath(in.getPath());
    out.setSecure(in.isSecure());
    out.setValue(in.getValue());
    out.setVersion(1);
    return out;
  }

  private static Cookie convert(org.apache.http.cookie.Cookie in) {
    return new Cookie(in.getName(),
        in.getValue(),
        in.getDomain(),
        in.getPath(),
        in.getExpiryDate(),
        in.isSecure());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addCookie(Cookie cookie) {
    cookieStore.addCookie(convert(cookie));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteAllCookies() {
    cookieStore.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteCookie(Cookie cookie) {
    List<org.apache.http.cookie.Cookie> cookies = cookieStore.getCookies();
    String toDelete = new StringBuilder().append(cookie.getDomain().toLowerCase())
        .append("\n").append(cookie.getName().toLowerCase())
        .append("\n").append(cookie.getPath().toLowerCase()).toString();
    for (org.apache.http.cookie.Cookie cur : cookies) {
      String curString = new StringBuilder().append(cur.getDomain().toLowerCase())
          .append("\n").append(cur.getName().toLowerCase())
          .append("\n").append(cur.getPath().toLowerCase()).toString();
      if (toDelete.equals(curString)) {
        removeFromCookieStore(cur);
      }
    }
  }

  private void removeFromCookieStore(org.apache.http.cookie.Cookie cookie) {
    BasicClientCookie tmp = new BasicClientCookie(cookie.getName(), "");
    tmp.setDomain(cookie.getDomain());
    tmp.setPath(cookie.getPath());
    tmp.setExpiryDate(new Date(0));
    cookieStore.addCookie(tmp);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteCookieNamed(String name) {
    for (org.apache.http.cookie.Cookie cur : cookieStore.getCookies()) {
      if (cur.getName().equals(name)) {
        removeFromCookieStore(cur);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Cookie getCookieNamed(String name) {
    for (org.apache.http.cookie.Cookie cur : cookieStore.getCookies()) {
      if (cur.getName().equals(name)) {
        return convert(cur);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Cookie> getCookies() {
    Set<Cookie> cookies = new LinkedHashSet<Cookie>();
    for (org.apache.http.cookie.Cookie cur : cookieStore.getCookies()) {
      cookies.add(convert(cur));
    }
    return cookies;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ImeHandlerServer ime() {
    return imeHandler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LogsServer logs() {
    return com.machinepublishers.jbrowserdriver.LogsServer.instance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimeoutsServer timeouts() {
    return timeouts.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WindowServer window() {
    return context.item().window.get();
  }

}
