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

import java.io.IOException;
import java.io.Serializable;
import java.net.CookieHandler;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicHeader;

class CookieStore extends CookieHandler implements org.apache.http.client.CookieStore, Serializable {

  private static final CookieSpec spec = new LaxCookieSpecProvider().create(null);
  private final org.apache.http.client.CookieStore store = new BasicCookieStore();

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
    final String reqHost = canonicalHost(uri.getHost());
    final String reqPath = canonicalPath(uri.getPath());
    final boolean reqSecure = isSecure(uri.getScheme());
    final boolean reqJavascript = isJavascript(uri.getScheme());
    StringBuilder builder = new StringBuilder();
    if (reqJavascript) {
      List<Cookie> list;
      synchronized (store) {
        store.clearExpired(new Date());
        list = store.getCookies();
      }
      for (Cookie cookie : list) {
        if ((!cookie.isSecure() || reqSecure)
            && reqHost.endsWith(canonicalHost(cookie.getDomain()))
            && reqPath.startsWith(canonicalPath(cookie.getPath()))) {
          if (builder.length() > 0) {
            builder.append(';');
          }
          builder.append(cookie.getName());
          builder.append('=');
          builder.append(cookie.getValue());
        }
      }
    }
    String cookies = builder.length() == 0 ? null : builder.toString();
    Map<String, List<String>> map;
    if (cookies != null) {
      map = new HashMap<String, List<String>>();
      map.put("Cookie", Arrays.asList(cookies));
    } else {
      map = Collections.emptyMap();
    }
    return map;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
    if (isJavascript(uri.getScheme())) {
      for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
        for (String value : entry.getValue()) {
          try {
            List<Cookie> cookies = spec.parse(new BasicHeader(entry.getKey(), value),
                new CookieOrigin(
                    uri.getHost(),
                    canonicalPort(uri.getScheme(), uri.getPort()),
                    canonicalPath(uri.getPath()),
                    isSecure(uri.getScheme())));
            for (Cookie cookie : cookies) {
              synchronized (store) {
                store.addCookie(cookie);
              }
            }
          } catch (MalformedCookieException e) {
            LogsServer.instance().warn(
                "Malformed cookie for cookie named " + entry.getKey() + ". " + e.getMessage());
          }
        }
      }
    }
  }

  private static boolean isSecure(String scheme) {
    return "https".equalsIgnoreCase(scheme) || "javascripts".equalsIgnoreCase(scheme);
  }

  private static boolean isJavascript(String scheme) {
    return "javascripts".equalsIgnoreCase(scheme) || "javascript".equalsIgnoreCase(scheme);
  }

  private static int canonicalPort(String scheme, int port) {
    if (port > -1) {
      return port;
    }
    return isSecure(scheme) ? 443 : 80;
  }

  private static String canonicalHost(String host) {
    if (host == null || host.isEmpty()) {
      return "";
    }
    return host.startsWith(".") ? host.toLowerCase() : "." + host.toLowerCase();
  }

  private static String canonicalPath(String path) {
    String canonical = StringUtils.isEmpty(path) ? "/" : path;
    canonical = canonical.startsWith("/") ? canonical : "/" + canonical;
    canonical = canonical.endsWith("/") ? canonical : canonical + "/";
    return canonical.toLowerCase();
  }
  
  public void addCsrfHeaders(Settings settings, HttpRequestBase req) {
    final String reqHost = canonicalHost(req.getURI().getHost());
	final String reqPath = canonicalPath(req.getURI().getPath());
	final boolean reqSecure = isSecure(req.getURI().getScheme());
	    
	List<Cookie> list;
    synchronized (store) {
      list = store.getCookies();
    }
    String csrfToken = null;
    for (Cookie cookie : list) {
      if ((!cookie.isSecure() || reqSecure)
          && reqHost.endsWith(canonicalHost(cookie.getDomain()))
          && reqPath.startsWith(canonicalPath(cookie.getPath()))) {
      	if(SettingsManager.settings().getCsrfResponseToken() != null && 
       		  cookie.getName().equalsIgnoreCase(SettingsManager.settings().getCsrfResponseToken())) {
       	  csrfToken = cookie.getValue();
       	  break;
        }
      }
    }
    if(csrfToken != null) {
      req.addHeader(SettingsManager.settings().getCsrfRequestToken(), csrfToken);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addCookie(Cookie cookie) {
    synchronized (store) {
      store.addCookie(cookie);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Cookie> getCookies() {
    synchronized (store) {
      return store.getCookies();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean clearExpired(Date date) {
    synchronized (store) {
      return store.clearExpired(date);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    synchronized (store) {
      store.clear();
    }
  }
}
