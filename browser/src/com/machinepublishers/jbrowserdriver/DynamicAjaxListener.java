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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.webkit.LoadListenerClient;

class DynamicAjaxListener implements Runnable {
  private static final long WAIT_INTERVAL = 1000;
  private static final long MAX_WAIT_DEFAULT = 15000;
  private final int state;
  private final Integer newStatusCode;
  private final AtomicInteger statusCode;
  private final Object statusMonitor;
  private final Method clearStatusMonitor;
  private final AtomicInteger resourceCount;
  private final AtomicBoolean superseded;
  private final long timeoutMS;

  DynamicAjaxListener(final int state, final int newStatusCode,
      final AtomicInteger statusCode, final Object statusMonitor,
      final Method clearStatusMonitor, final AtomicInteger resourceCount, final long timeoutMS) {
    this.state = state;
    this.newStatusCode = newStatusCode;
    this.statusCode = statusCode;
    this.statusMonitor = statusMonitor;
    this.clearStatusMonitor = clearStatusMonitor;
    this.resourceCount = resourceCount;
    this.timeoutMS = timeoutMS <= 0 ? MAX_WAIT_DEFAULT : timeoutMS;
    this.superseded = new AtomicBoolean();
  }

  DynamicAjaxListener(final AtomicInteger statusCode, final AtomicInteger resourceCount,
      final AtomicBoolean superseded, final long timeoutMS) {
    this.statusCode = statusCode;
    this.resourceCount = resourceCount;
    this.superseded = superseded;
    this.timeoutMS = timeoutMS <= 0 ? MAX_WAIT_DEFAULT : timeoutMS;
    this.state = -1;
    this.newStatusCode = null;
    this.statusMonitor = null;
    this.clearStatusMonitor = null;
  }

  @Override
  public void run() {
    if (Thread.interrupted()) {
      return;
    }
    if (newStatusCode == null || state == LoadListenerClient.PAGE_FINISHED) {
      int totalWait = 0;
      do {
        try {
          Thread.sleep(WAIT_INTERVAL);
        } catch (InterruptedException e) {}
        if (Thread.interrupted()) {
          return;
        }
        totalWait += WAIT_INTERVAL;
      } while (resourceCount.get() > 0 && totalWait < timeoutMS);
    }
    synchronized (statusCode) {
      if (newStatusCode == null) {
        synchronized (superseded) {
          if (!superseded.get() && !Thread.interrupted()) {
            resourceCount.set(0);
            statusCode.set(200);
            statusCode.notifyAll();
          }
        }
      } else {
        resourceCount.set(0);
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