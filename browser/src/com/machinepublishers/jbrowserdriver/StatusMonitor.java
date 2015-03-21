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

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class StatusMonitor {
  private final static Map<Long, StatusMonitor> instances = new HashMap<Long, StatusMonitor>();
  private final Object lock = new Object();
  private final Map<String, StreamConnection> connections = new HashMap<String, StreamConnection>();
  private final Set<String> primaryDocuments = new HashSet<String>();
  private final Set<String> discarded = new HashSet<String>();
  private boolean monitoring = false;

  private StatusMonitor() {}

  static synchronized StatusMonitor get(long settingsId) {
    if (!instances.containsKey(settingsId)) {
      instances.put(settingsId, new StatusMonitor());
    }
    return instances.get(settingsId);
  }

  static synchronized void remove(long settingsId) {
    instances.remove(settingsId);
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
    synchronized (lock) {
      monitoring = false;
      int code = 499;
      if (connections.containsKey(url)) {
        try {
          code = connections.get(url).getResponseCode();
          code = code <= 0 ? 499 : code;
        } catch (Throwable t) {
          Logs.exception(t);
        }
      }
      return code;
    }
  }

  void clearStatusMonitor() {
    synchronized (lock) {
      connections.clear();
      primaryDocuments.clear();
      discarded.clear();
    }
  }
}
