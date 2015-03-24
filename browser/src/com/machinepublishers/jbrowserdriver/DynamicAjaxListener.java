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

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.webkit.LoadListenerClient;

class DynamicAjaxListener implements Runnable {
  private static final int WAIT_COUNT = 5;
  private static final long WAIT_INTERVAL =
      Long.parseLong(System.getProperty("jbd.ajaxwait", "600")) / WAIT_COUNT;
  private static final long MAX_WAIT_DEFAULT = 15000;
  private final int state;
  private final AtomicInteger waitCount = new AtomicInteger();
  private final Integer newStatusCode;
  private final AtomicInteger statusCode;
  private final Object statusMonitor;
  private final Method clearStatusMonitor;
  private final Set<String> resources;
  private final AtomicBoolean superseded;
  private final long timeoutMS;

  DynamicAjaxListener(final int state, final int newStatusCode,
      final AtomicInteger statusCode, final Object statusMonitor,
      final Method clearStatusMonitor, final Set<String> resources, final long timeoutMS) {
    this.state = state;
    this.newStatusCode = newStatusCode;
    this.statusCode = statusCode;
    this.statusMonitor = statusMonitor;
    this.clearStatusMonitor = clearStatusMonitor;
    this.resources = resources;
    this.timeoutMS = timeoutMS <= 0 ? MAX_WAIT_DEFAULT : timeoutMS;
    this.superseded = new AtomicBoolean();
  }

  DynamicAjaxListener(final AtomicInteger statusCode, final Set<String> resources,
      final AtomicBoolean superseded, final long timeoutMS) {
    this.statusCode = statusCode;
    this.resources = resources;
    this.superseded = superseded;
    this.timeoutMS = timeoutMS <= 0 ? MAX_WAIT_DEFAULT : timeoutMS;
    this.state = -1;
    this.newStatusCode = null;
    this.statusMonitor = null;
    this.clearStatusMonitor = null;
  }

  @Override
  public void run() {
    if (superseded.get() || Thread.interrupted()) {
      return;
    }
    if (newStatusCode == null || state == LoadListenerClient.PAGE_FINISHED) {
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