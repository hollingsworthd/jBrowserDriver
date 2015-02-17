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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javafx.scene.web.WebView;

import com.sun.webkit.LoadListenerClient;

class DynamicHttpListener implements LoadListenerClient {
  private final WebView view;
  private final AtomicInteger statusCode;
  private final long settingsId;
  private final AtomicLong frame = new AtomicLong();
  private static final boolean TRACE = "true".equals(System.getProperty("jbd.trace"));
  private static final Method getStatusMonitor;
  private static final Method resetStatusMonitor;
  private static final Method stopStatusMonitor;
  static {
    Method getStatusMonitorTmp = null;
    Method resetStatusMonitorTmp = null;
    Method stopStatusMonitorTmp = null;
    try {
      Class statusMonitor = DynamicHttpListener.class.getClassLoader().loadClass("com.machinepublishers.jbrowserdriver.StatusMonitor");
      getStatusMonitorTmp = statusMonitor.getDeclaredMethod("get", long.class);
      getStatusMonitorTmp.setAccessible(true);
      resetStatusMonitorTmp = statusMonitor.getDeclaredMethod("resetStatusMonitor", String.class);
      resetStatusMonitorTmp.setAccessible(true);
      stopStatusMonitorTmp = statusMonitor.getDeclaredMethod("stopStatusMonitor", String.class);
      stopStatusMonitorTmp.setAccessible(true);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    getStatusMonitor = getStatusMonitorTmp;
    resetStatusMonitor = resetStatusMonitorTmp;
    stopStatusMonitor = stopStatusMonitorTmp;
  }

  DynamicHttpListener(WebView view, AtomicInteger statusCode, long settingsId) {
    this.view = view;
    this.statusCode = statusCode;
    this.settingsId = settingsId;
  }

  private void trace(String label, long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    System.out.println(settingsId
        + "-" + label + "-> " + url
        + " ** {state: " + state
        + ", progress: " + progress
        + ", error: " + errorCode
        + ", contentType: "
        + contentType
        + ", frame: " + frame
        + "}");
  }

  @Override
  public void dispatchResourceLoadEvent(long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    if (TRACE) {
      trace("Rsrc", frame, state, url, contentType, progress, errorCode);
    }
  }

  @Override
  public void dispatchLoadEvent(long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    try {
      if ((this.frame.get() == 0 || this.frame.get() == frame)
          && (state == LoadListenerClient.PAGE_STARTED || state == LoadListenerClient.PAGE_REDIRECTED)) {
        statusCode.set(0);
        this.frame.set(frame);
        Object obj = getStatusMonitor.invoke(null, settingsId);
        resetStatusMonitor.invoke(obj, view.getEngine().getLocation());
      } else if ((this.frame.get() == 0 || this.frame.get() == frame)
          && (state == LoadListenerClient.PAGE_FINISHED
              || state == LoadListenerClient.LOAD_FAILED || state == LoadListenerClient.LOAD_STOPPED)) {
        this.frame.set(0);
        Object obj = getStatusMonitor.invoke(null, settingsId);
        int code = (Integer) stopStatusMonitor.invoke(obj, view.getEngine().getLocation());
        statusCode.set(state == LoadListenerClient.PAGE_FINISHED ? code : 499);
        synchronized (statusCode) {
          statusCode.notifyAll();
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    if (TRACE) {
      trace("Page", frame, state, url, contentType, progress, errorCode);
    }
  }
}
