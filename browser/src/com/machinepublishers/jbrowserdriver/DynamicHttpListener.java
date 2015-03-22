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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.webkit.LoadListenerClient;

class DynamicHttpListener implements LoadListenerClient {
  private final List<Thread> threadsFromReset = new ArrayList<Thread>();
  private final AtomicBoolean superseded = new AtomicBoolean();
  private final Set<String> resources = new HashSet<String>();
  private final AtomicInteger statusCode;
  private final long settingsId;
  private final AtomicLong frame = new AtomicLong();
  private final Object statusMonitor;
  private final AtomicLong timeoutMS;
  private static final boolean TRACE = "true".equals(System.getProperty("jbd.trace"));
  private static final Method getStatusMonitor;
  private static final Method startStatusMonitor;
  private static final Method stopStatusMonitor;
  private static final Method clearStatusMonitor;
  private static final Method originalFromRedirect;
  static {
    Method getStatusMonitorTmp = null;
    Method startStatusMonitorTmp = null;
    Method stopStatusMonitorTmp = null;
    Method clearStatusMonitorTmp = null;
    Method originalFromRedirectTmp = null;
    try {
      Class statusMonitorClass = DynamicHttpListener.class.getClassLoader().
          loadClass("com.machinepublishers.jbrowserdriver.StatusMonitor");
      getStatusMonitorTmp = statusMonitorClass.getDeclaredMethod("get", long.class);
      getStatusMonitorTmp.setAccessible(true);
      startStatusMonitorTmp = statusMonitorClass.getDeclaredMethod("startStatusMonitor", String.class);
      startStatusMonitorTmp.setAccessible(true);
      stopStatusMonitorTmp = statusMonitorClass.getDeclaredMethod("stopStatusMonitor", String.class);
      stopStatusMonitorTmp.setAccessible(true);
      clearStatusMonitorTmp = statusMonitorClass.getDeclaredMethod("clearStatusMonitor");
      clearStatusMonitorTmp.setAccessible(true);
      originalFromRedirectTmp = statusMonitorClass.getDeclaredMethod("originalFromRedirect", String.class);
      originalFromRedirectTmp.setAccessible(true);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    getStatusMonitor = getStatusMonitorTmp;
    startStatusMonitor = startStatusMonitorTmp;
    stopStatusMonitor = stopStatusMonitorTmp;
    clearStatusMonitor = clearStatusMonitorTmp;
    originalFromRedirect = originalFromRedirectTmp;
  }

  DynamicHttpListener(AtomicInteger statusCode, AtomicLong timeoutMS, long settingsId) {
    this.statusCode = statusCode;
    this.timeoutMS = timeoutMS;
    this.settingsId = settingsId;
    Object statusMonitorTmp = null;
    try {
      statusMonitorTmp = getStatusMonitor.invoke(null, settingsId);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    statusMonitor = statusMonitorTmp;
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
    if (url.startsWith("http://") || url.startsWith("https://")) {
      if (state == LoadListenerClient.RESOURCE_STARTED) {
        synchronized (resources) {
          resources.add(frame + url);
        }
      } else if (state == LoadListenerClient.RESOURCE_FINISHED
          || state == LoadListenerClient.RESOURCE_FAILED) {
        String original = null;
        try {
          original = (String) originalFromRedirect.invoke(statusMonitor, url);
        } catch (Throwable t) {
          t.printStackTrace();
        }
        synchronized (resources) {
          resources.remove(frame + url);
          if (original != null) {
            resources.remove(frame + original);
          }
        }
      }
    }
    if (TRACE) {
      trace("Rsrc", frame, state, url, contentType, progress, errorCode);
    }
  }

  public void resetStatusCode() {
    synchronized (threadsFromReset) {
      for (Thread thread : threadsFromReset) {
        thread.interrupt();
      }
      threadsFromReset.clear();
      synchronized (superseded) {
        superseded.set(false);
        statusCode.set(0);
        synchronized (resources) {
          resources.clear();
        }
      }
      Thread thread = new Thread(new DynamicAjaxListener(
          statusCode, resources, superseded, timeoutMS.get()));
      threadsFromReset.add(thread);
      thread.start();
    }
  }

  @Override
  public void dispatchLoadEvent(long frame, final int state, String url,
      String contentType, double progress, int errorCode) {
    try {
      this.frame.compareAndSet(0l, frame);
      if (this.frame.get() == frame
          && (url.startsWith("http://") || url.startsWith("https://"))) {
        if (state == LoadListenerClient.PAGE_STARTED
            || state == LoadListenerClient.PAGE_REDIRECTED) {
          synchronized (superseded) {
            statusCode.set(0);
            superseded.set(true);
            synchronized (resources) {
              resources.clear();
              resources.add(frame + url);
            }
          }
          startStatusMonitor.invoke(statusMonitor, url);
        } else if (statusCode.get() == 0
            && (state == LoadListenerClient.PAGE_FINISHED
            || state == LoadListenerClient.LOAD_FAILED)) {
          final int code = (Integer) stopStatusMonitor.invoke(statusMonitor, url);
          final int newStatusCode = state == LoadListenerClient.PAGE_FINISHED ? code : 499;
          synchronized (resources) {
            resources.remove(frame + url);
          }
          new Thread(new DynamicAjaxListener(state, newStatusCode, statusCode,
              statusMonitor, clearStatusMonitor, resources, timeoutMS.get())).start();
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
