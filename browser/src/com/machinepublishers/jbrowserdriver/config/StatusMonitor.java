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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.machinepublishers.jbrowserdriver.Logs;

public class StatusMonitor {
  private final static Map<Long, StatusMonitor> instances = new HashMap<Long, StatusMonitor>();
  private final Object lock = new Object();
  private final Map<String, StreamConnection> connections = new HashMap<String, StreamConnection>();
  private String primaryDocument = null;
  private boolean monitoring = false;

  private StatusMonitor() {}

  public static synchronized StatusMonitor get(long settingsId) {
    if (!instances.containsKey(settingsId)) {
      instances.put(settingsId, new StatusMonitor());
    }
    return instances.get(settingsId);
  }

  public static synchronized void remove(long settingsId) {
    instances.remove(settingsId);
  }

  boolean isPrimaryDocument(String url) {
    synchronized (lock) {
      return url.equalsIgnoreCase(primaryDocument);
    }
  }

  public void resetStatusMonitor(String url) {
    synchronized (lock) {
      monitoring = true;
      primaryDocument = url;
    }
  }

  void startStatusMonitor(URL url, StreamConnection conn) {
    synchronized (lock) {
      if (monitoring) {
        connections.put(url.toExternalForm(), conn);
      }
    }
  }

  public int stopStatusMonitor(String url) {
    synchronized (lock) {
      monitoring = false;
      int code = 0;
      if (connections.containsKey(url)) {
        try {
          code = connections.get(url).getResponseCode();
        } catch (Throwable t) {
          Logs.exception(t);
        }
      }
      connections.clear();
      return code;
    }
  }
}
