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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.webkit.LoadListenerClient;

class DynamicHttpListener implements LoadListenerClient {
  private static final Map<Integer, String> states;
  private static final Map<Integer, String> errors;
  static {
    Map<Integer, String> statesTmp = new HashMap<Integer, String>();
    statesTmp.put(LoadListenerClient.PAGE_STARTED, "page_started");
    statesTmp.put(LoadListenerClient.PAGE_FINISHED, "page_finished");
    statesTmp.put(LoadListenerClient.PAGE_REDIRECTED, "page_redirected");
    statesTmp.put(LoadListenerClient.LOAD_FAILED, "load_failed");
    statesTmp.put(LoadListenerClient.LOAD_STOPPED, "load_stopped");
    statesTmp.put(LoadListenerClient.CONTENT_RECEIVED, "content_received");
    statesTmp.put(LoadListenerClient.TITLE_RECEIVED, "title_received");
    statesTmp.put(LoadListenerClient.ICON_RECEIVED, "icon_received");
    statesTmp.put(LoadListenerClient.CONTENTTYPE_RECEIVED, "contenttype_received");
    statesTmp.put(LoadListenerClient.DOCUMENT_AVAILABLE, "document_available");
    statesTmp.put(LoadListenerClient.RESOURCE_STARTED, "resource_started");
    statesTmp.put(LoadListenerClient.RESOURCE_REDIRECTED, "resource_redirected");
    statesTmp.put(LoadListenerClient.RESOURCE_FINISHED, "resource_finished");
    statesTmp.put(LoadListenerClient.RESOURCE_FAILED, "resource_failed");
    statesTmp.put(LoadListenerClient.PROGRESS_CHANGED, "progress_changed");
    states = Collections.unmodifiableMap(statesTmp);

    Map<Integer, String> errorsTmp = new HashMap<Integer, String>();
    errorsTmp.put(0, "none");
    errorsTmp.put(LoadListenerClient.UNKNOWN_HOST, "unknown_host");
    errorsTmp.put(LoadListenerClient.MALFORMED_URL, "malformed_url");
    errorsTmp.put(LoadListenerClient.SSL_HANDSHAKE, "ssl_handshake");
    errorsTmp.put(LoadListenerClient.CONNECTION_REFUSED, "connection_refused");
    errorsTmp.put(LoadListenerClient.CONNECTION_RESET, "connection_reset");
    errorsTmp.put(LoadListenerClient.NO_ROUTE_TO_HOST, "no_route_to_host");
    errorsTmp.put(LoadListenerClient.CONNECTION_TIMED_OUT, "connection_timed_out");
    errorsTmp.put(LoadListenerClient.PERMISSION_DENIED, "permission_denied");
    errorsTmp.put(LoadListenerClient.INVALID_RESPONSE, "invalid_response");
    errorsTmp.put(LoadListenerClient.TOO_MANY_REDIRECTS, "too_many_redirects");
    errorsTmp.put(LoadListenerClient.FILE_NOT_FOUND, "file_not_found");
    errorsTmp.put(LoadListenerClient.UNKNOWN_ERROR, "unknown_error");
    errors = Collections.unmodifiableMap(errorsTmp);
  }
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
        + ", state: " + states.get(state)
        + ", progress: " + progress
        + ", error: " + errors.get(errorCode)
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
