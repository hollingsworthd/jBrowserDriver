/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class DynamicAjaxListener implements Runnable {
  private static final int WAIT_COUNT = 5;
  private static final long WAIT_INTERVAL =
      Long.parseLong(System.getProperty("jbd.ajaxwait", "600")) / WAIT_COUNT;
  private static final long RESOURCE_TIMEOUT =
      Long.parseLong(System.getProperty("jbd.resourcetimeout", "5000")) / WAIT_COUNT;
  private static final long MAX_WAIT_DEFAULT = 15000;
  private final AtomicInteger waitCount = new AtomicInteger();
  private final Integer newStatusCode;
  private final AtomicInteger statusCode;
  private final Object statusMonitor;
  private final Method clearStatusMonitor;
  private final Map<String, Long> resources;
  private final AtomicBoolean superseded;
  private final long timeoutMS;

  DynamicAjaxListener(final int newStatusCode,
      final AtomicInteger statusCode, final Object statusMonitor,
      final Method clearStatusMonitor, final Map<String, Long> resources, final long timeoutMS) {
    this.newStatusCode = newStatusCode;
    this.statusCode = statusCode;
    this.statusMonitor = statusMonitor;
    this.clearStatusMonitor = clearStatusMonitor;
    this.resources = resources;
    this.timeoutMS = timeoutMS <= 0 ? MAX_WAIT_DEFAULT : timeoutMS;
    this.superseded = new AtomicBoolean();
  }

  DynamicAjaxListener(final AtomicInteger statusCode, final Map<String, Long> resources,
      final AtomicBoolean superseded, final long timeoutMS) {
    this.statusCode = statusCode;
    this.resources = resources;
    this.superseded = superseded;
    this.timeoutMS = timeoutMS <= 0 ? MAX_WAIT_DEFAULT : timeoutMS;
    this.newStatusCode = null;
    this.statusMonitor = null;
    this.clearStatusMonitor = null;
  }

  private void pruneResources() {
    final long time = System.currentTimeMillis();
    synchronized (resources) {
      final Set<String> remove = new HashSet<String>();
      for (Map.Entry<String, Long> entry : resources.entrySet()) {
        if (time - entry.getValue() > RESOURCE_TIMEOUT) {
          remove.add(entry.getKey());
        }
      }
      for (String key : remove) {
        resources.remove(key);
      }
    }
  }

  @Override
  public void run() {
    if (superseded.get() || Thread.interrupted()) {
      return;
    }
    int totalWait = 0;
    int size = 0;
    boolean idle = false;
    waitCount.set(0);
    while (!idle && totalWait < timeoutMS) {
      try {
        Thread.sleep(WAIT_INTERVAL);
      } catch (InterruptedException e) {}
      if (superseded.get() || Thread.interrupted()) {
        return;
      }
      totalWait += WAIT_INTERVAL;
      synchronized (resources) {
        pruneResources();
        size = resources.size();
      }
      if (size > 0) {
        idle = false;
        waitCount.set(0);
      } else if (waitCount.get() == WAIT_COUNT) {
        idle = true;
      } else {
        idle = false;
        waitCount.incrementAndGet();
      }
    }
    synchronized (statusCode) {
      if (newStatusCode == null) {
        synchronized (superseded) {
          if (!superseded.get() && !Thread.interrupted()) {
            synchronized (resources) {
              resources.clear();
            }
            statusCode.set(200);
            statusCode.notifyAll();
          }
        }
      } else {
        synchronized (resources) {
          resources.clear();
        }
        statusCode.set(newStatusCode);
        try {
          clearStatusMonitor.invoke(statusMonitor);
        } catch (Throwable t) {
          t.printStackTrace();
        }
        statusCode.notifyAll();
      }
    }
  }
}