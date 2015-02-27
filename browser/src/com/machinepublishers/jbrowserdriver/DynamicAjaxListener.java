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

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.webkit.LoadListenerClient;

class DynamicAjaxListener implements Runnable {
  private static final int WAIT = 150;
  private static final int MAX_WAIT = 15000;
  private final int state;
  private final int newStatusCode;
  private final AtomicInteger statusCode;
  private final AtomicInteger resourceCount;

  DynamicAjaxListener(final int state, final int newStatusCode,
      final AtomicInteger statusCode, final AtomicInteger resourceCount) {
    this.state = state;
    this.newStatusCode = newStatusCode;
    this.statusCode = statusCode;
    this.resourceCount = resourceCount;
  }

  @Override
  public void run() {
    if (state == LoadListenerClient.PAGE_FINISHED) {
      int totalWait = 0;
      do {
        try {
          Thread.sleep(WAIT);
        } catch (InterruptedException e) {}
        totalWait += WAIT;
      } while (resourceCount.get() > 0 && totalWait < MAX_WAIT);
    }
    resourceCount.set(0);
    synchronized (statusCode) {
      if (newStatusCode > -1) {
        statusCode.set(newStatusCode);
      }
      statusCode.notifyAll();
    }
  }
}