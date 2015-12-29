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

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class StatusMonitor {
  private static final StatusMonitor instance = new StatusMonitor();
  private final Object lock = new Object();
  private final Map<String, StreamConnection> connections = new HashMap<String, StreamConnection>();
  private final Set<String> primaryDocuments = new HashSet<String>();
  private final Set<String> discarded = new HashSet<String>();
  private final Map<String, String> redirects = new HashMap<String, String>();
  private boolean monitoring;

  static StatusMonitor instance() {
    return instance;
  }

  boolean isPrimaryDocument(String url) {
    synchronized (lock) {
      return primaryDocuments.contains(url);
    }
  }

  boolean isDiscarded(String url) {
    synchronized (lock) {
      return discarded.contains(url);
    }
  }

  void addRedirect(String original, String redirected) {
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
      return redirects.get(redirected);
    }
  }

  void startStatusMonitor(String url) {
    synchronized (lock) {
      monitoring = true;
      primaryDocuments.add(url);
    }
  }

  void addStatusMonitor(URL url, StreamConnection conn) {
    synchronized (lock) {
      if (monitoring) {
        connections.put(url.toExternalForm(), conn);
      }
    }
  }

  void addDiscarded(String url) {
    synchronized (lock) {
      discarded.add(url);
    }
  }

  int stopStatusMonitor(String url) {
    StreamConnection conn = null;
    synchronized (lock) {
      monitoring = false;
      conn = connections.get(url);
    }
    int code = 499;
    if (conn != null) {
      try {
        code = conn.getResponseCode();
        code = code <= 0 ? 499 : code;
      } catch (Throwable t) {
        Logs.instance().exception(t);
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
      }
    }
  }
}
