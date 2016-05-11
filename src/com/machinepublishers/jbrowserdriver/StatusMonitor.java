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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class StatusMonitor {
  private static final StatusMonitor instance = new StatusMonitor();
  private final Object lock = new Object();
  private final Map<String, StreamConnection> connections = new HashMap<String, StreamConnection>();
  private final Set<String> primaryDocuments = new HashSet<String>();
  private final Set<String> discarded = new HashSet<String>();
  private final Map<String, String> redirects = new HashMap<String, String>();
  private final List<String> startedUrls = new ArrayList<String>();
  private boolean monitoring;

  static StatusMonitor instance() {
    return instance;
  }

  private static String canonicalUrl(String url) {
    if (url == null) {
      return null;
    }
    int fragmentIndex = url.indexOf("#");
    url = fragmentIndex > -1 ? url.substring(0, fragmentIndex) : url;
    return url.endsWith("/") ? url : new StringBuilder().append(url).append("/").toString();
  }

  boolean isPrimaryDocument(String url) {
    synchronized (lock) {
      return primaryDocuments.contains(canonicalUrl(url));
    }
  }

  boolean isDiscarded(String url) {
    synchronized (lock) {
      return discarded.contains(canonicalUrl(url));
    }
  }

  void addRedirect(String original, String redirected) {
    original = canonicalUrl(original);
    redirected = canonicalUrl(redirected);
    if (original != null
        && redirected != null
        && !original.equals(redirected)) {
      synchronized (lock) {
        redirects.put(redirected, original);
      }
    }
  }

  String originalFromRedirect(String redirected) {
    synchronized (lock) {
      return redirects.get(canonicalUrl(redirected));
    }
  }

  void startStatusMonitor(String url) {
    synchronized (lock) {
      monitoring = true;
      startedUrls.add(canonicalUrl(url));
    }
  }

  void addPrimaryDocument(String url) {
    synchronized (lock) {
      primaryDocuments.add(canonicalUrl(url));
    }
  }

  void addStatusMonitor(URL url, StreamConnection conn) {
    synchronized (lock) {
      if (monitoring) {
        connections.put(canonicalUrl(url.toExternalForm()), conn);
      }
    }
  }

  void addDiscarded(String url) {
    synchronized (lock) {
      discarded.add(canonicalUrl(url));
    }
  }

  int stopStatusMonitor(String url) {
    StreamConnection conn = null;
    synchronized (lock) {
      monitoring = false;
      conn = connections.get(canonicalUrl(url));
      for (int i = startedUrls.size() - 1; conn == null && i > -1; i--) {
        conn = connections.get(startedUrls.get(i));
      }
    }
    int code = 499;
    if (conn != null) {
      try {
        code = conn.getResponseCode();
        code = code <= 0 ? 499 : code;
      } catch (Throwable t) {
        LogsServer.instance().exception(t);
      }
    }
    return code;
  }

  void clearStatusMonitor() {
    synchronized (lock) {
      if (!monitoring) {
        for (StreamConnection conn : connections.values()) {
          Util.close(conn);
        }
        StreamConnection.cleanUp();
        connections.clear();
        primaryDocuments.clear();
        discarded.clear();
        redirects.clear();
        startedUrls.clear();
      }
    }
  }
}
