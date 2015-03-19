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
  private static final int WAIT = 1000;
  private static final int MAX_WAIT = 15000;
  private static final int START_WAIT = 1000;
  private final int state;
  private final int newStatusCode;
  private final AtomicInteger statusCode;
  private final Object statusMonitor;
  private final Method clearStatusMonitor;
  private final AtomicInteger resourceCount;
  private final AtomicBoolean started;
  private final AtomicBoolean stop;

  DynamicAjaxListener(final int state, final int newStatusCode, final AtomicInteger statusCode,
      final Object statusMonitor, final Method clearStatusMonitor, final AtomicInteger resourceCount) {
    this.state = state;
    this.newStatusCode = newStatusCode;
    this.statusCode = statusCode;
    this.statusMonitor = statusMonitor;
    this.clearStatusMonitor = clearStatusMonitor;
    this.resourceCount = resourceCount;
    this.started = null;
    this.stop = new AtomicBoolean();
  }

  DynamicAjaxListener(final AtomicInteger statusCode, final AtomicInteger resourceCount,
      final AtomicBoolean started, final AtomicBoolean stop) {
    this.statusCode = statusCode;
    this.resourceCount = resourceCount;
    this.started = started;
    this.stop = stop;
    this.state = -1;
    this.newStatusCode = -1;
    this.statusMonitor = null;
    this.clearStatusMonitor = null;
  }

  @Override
  public void run() {
    if (started != null) {
      synchronized (started) {
        if (!started.get()) {
          try {
            started.wait(START_WAIT);
          } catch (InterruptedException e) {}
        }
      }
    }
    if (Thread.interrupted()) {
      return;
    }
    if ((started == null && state == LoadListenerClient.PAGE_FINISHED)
        || (started != null && started.get())) {
      int totalWait = 0;
      do {
        try {
          Thread.sleep(WAIT);
        } catch (InterruptedException e) {}
        totalWait += WAIT;
        if (Thread.interrupted()) {
          return;
        }
      } while (resourceCount.get() > 0 && totalWait < MAX_WAIT);
    }
    if (!stop.get()) {
      if (Thread.interrupted()) {
        return;
      }
      resourceCount.set(0);
    }
    synchronized (statusCode) {
      if (started == null) {
        if (newStatusCode > -1) {
          statusCode.set(newStatusCode);
        }
        try {
          clearStatusMonitor.invoke(statusMonitor);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      } else {
        if (!stop.get()) {
          if (Thread.interrupted()) {
            return;
          }
          statusCode.set(200);
        }
      }
      if (!stop.get()) {
        if (Thread.interrupted()) {
          return;
        }
        statusCode.notifyAll();
      }
    }
  }
}