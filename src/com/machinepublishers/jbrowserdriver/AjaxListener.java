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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class AjaxListener implements Runnable {
  private static final long WAIT_INTERVAL = Long.parseLong(System.getProperty("jbd.ajaxwait", "120"));
  private static final long RESOURCE_TIMEOUT = Long.parseLong(System.getProperty("jbd.ajaxresourcetimeout", "2000"));
  private static final long MAX_WAIT_DEFAULT = 15000;
  private final Integer newStatusCode;
  private final AtomicInteger statusCode;
  private final Map<String, Long> resources;
  private final AtomicBoolean superseded;
  private final long timeoutMS;

  AjaxListener(final int newStatusCode,
      final AtomicInteger statusCode,
      final Map<String, Long> resources, final long timeoutMS) {
    this.newStatusCode = newStatusCode;
    this.statusCode = statusCode;
    this.resources = resources;
    this.timeoutMS = timeoutMS <= 0 ? MAX_WAIT_DEFAULT : timeoutMS;
    this.superseded = new AtomicBoolean();
  }

  AjaxListener(final AtomicInteger statusCode,
      final Map<String, Long> resources,
      final AtomicBoolean superseded, final long timeoutMS) {
    this.statusCode = statusCode;
    this.resources = resources;
    this.superseded = superseded;
    this.timeoutMS = timeoutMS <= 0 ? MAX_WAIT_DEFAULT : timeoutMS;
    this.newStatusCode = null;
  }

  @Override
  public void run() {
    int size = 0;
    final long start = System.currentTimeMillis();
    long time = start;
    while (time - start < timeoutMS) {
      try {
        Thread.sleep(WAIT_INTERVAL);
      } catch (InterruptedException e) {}
      time = System.currentTimeMillis();
      synchronized (statusCode) {
        if (superseded.get() || Thread.interrupted()) {
          return;
        }
        final Set<String> remove = new HashSet<String>();
        for (Map.Entry<String, Long> entry : resources.entrySet()) {
          if (time - entry.getValue() > RESOURCE_TIMEOUT) {
            remove.add(entry.getKey());
          }
        }
        for (String key : remove) {
          resources.remove(key);
        }
        size = resources.size();
      }
      if (size == 0) {
        break;
      }
    }
    synchronized (statusCode) {
      if (superseded.get() || Thread.interrupted()) {
        return;
      }
      if (newStatusCode == null) {
        resources.clear();
        statusCode.set(200);
        statusCode.notifyAll();
      } else {
        resources.clear();
        statusCode.set(newStatusCode);
        try {
          StatusMonitor.instance().clearStatusMonitor();
        } catch (Throwable t) {
          t.printStackTrace();
        }
        statusCode.notifyAll();
      }
    }
  }
}