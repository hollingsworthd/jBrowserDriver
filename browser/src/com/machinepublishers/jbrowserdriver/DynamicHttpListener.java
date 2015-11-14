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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.webkit.LoadListenerClient;

class DynamicHttpListener implements LoadListenerClient {
  private final List<Thread> threadsFromReset = new ArrayList<Thread>();
  private final AtomicBoolean superseded = new AtomicBoolean();
  private final Map<String, Long> resources = new HashMap<String, Long>();
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
        + " ** {timestamp: " + System.currentTimeMillis()
        + ", state: " + state
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
    synchronized (statusCode) {
      if (url.startsWith("http://") || url.startsWith("https://")) {
        if (state == LoadListenerClient.RESOURCE_STARTED) {
          resources.put(frame + url, System.currentTimeMillis());
        } else if (state == LoadListenerClient.RESOURCE_FINISHED
            || state == LoadListenerClient.RESOURCE_FAILED) {
          String original = null;
          try {
            original = (String) originalFromRedirect.invoke(statusMonitor, url);
          } catch (Throwable t) {
            t.printStackTrace();
          }
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
    synchronized (statusCode) {
      for (Thread thread : threadsFromReset) {
        thread.interrupt();
      }
      threadsFromReset.clear();
      superseded.set(false);
      statusCode.set(0);
      resources.clear();
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
      synchronized (statusCode) {
        this.frame.compareAndSet(0l, frame);
        if (this.frame.get() == frame) {
          if (state == LoadListenerClient.PAGE_STARTED
              || state == LoadListenerClient.PAGE_REDIRECTED
              || state == LoadListenerClient.DOCUMENT_AVAILABLE) {
            statusCode.set(0);
            superseded.set(true);
            resources.clear();
            resources.put(frame + url, System.currentTimeMillis());
            startStatusMonitor.invoke(statusMonitor, url);
          } else if (statusCode.get() == 0
              && (state == LoadListenerClient.PAGE_FINISHED
                  || state == LoadListenerClient.LOAD_STOPPED
                  || state == LoadListenerClient.LOAD_FAILED)) {
            final int code = (Integer) stopStatusMonitor.invoke(statusMonitor, url);
            final int newStatusCode = state == LoadListenerClient.PAGE_FINISHED ? code : 499;
            resources.remove(frame + url);
            new Thread(new DynamicAjaxListener(newStatusCode, statusCode,
                statusMonitor, clearStatusMonitor, resources, timeoutMS.get())).start();
          }
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
