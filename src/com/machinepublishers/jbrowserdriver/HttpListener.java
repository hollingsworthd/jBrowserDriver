/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC and the jBrowserDriver contributors
 * https://github.com/MachinePublishers/jBrowserDriver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.machinepublishers.jbrowserdriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.LoadListenerClient;

class HttpListener implements LoadListenerClient {
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

  private final List<Thread> ajaxListeners = new ArrayList<Thread>();
  private final Map<String, Long> resources = new HashMap<String, Long>();
  private final ContextItem contextItem;
  private final AtomicInteger statusCode;
  private final AtomicLong timeoutMS;
  private final StatusMonitor statusMonitor;
  private final LogsServer logs;

  HttpListener(ContextItem contextItem, AtomicInteger statusCode, AtomicLong timeoutMS) {
    this.contextItem = contextItem;
    this.statusCode = statusCode;
    this.timeoutMS = timeoutMS;
    this.statusMonitor = StatusMonitor.instance();
    this.logs = LogsServer.instance();
  }

  private void trace(String label, long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    if (state != LoadListenerClient.PROGRESS_CHANGED) {
      logs.trace(new StringBuilder()
          .append("-").append(label).append("-> ")
          .append(url)
          .append(" ** {timestamp: ").append(System.currentTimeMillis())
          .append(", state: ").append(states.get(state))
          .append(", progress: ").append(progress)
          .append(", error: ").append(errors.get(errorCode))
          .append(", contentType: ").append(contentType)
          .append(", frame: ").append(frame).append("}").toString());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispatchResourceLoadEvent(long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    final Settings settings = SettingsManager.settings();
    if (settings == null) {
      throw new RuntimeException("Request made after browser closed. Ignoring...");
    }
    synchronized (statusCode) {
      if (url.startsWith("http://") || url.startsWith("https://")) {
        if (state == LoadListenerClient.RESOURCE_STARTED) {
          resources.put(frame + url, System.currentTimeMillis());
        } else if (state == LoadListenerClient.RESOURCE_FINISHED
            || state == LoadListenerClient.RESOURCE_FAILED) {
          String original = null;
          original = statusMonitor.originalFromRedirect(url);
          resources.remove(frame + url);
          if (original != null) {
            resources.remove(frame + original);
          }
        }
      }
    }
    if ((settings.logTrace())
        && (url.startsWith("http://") || url.startsWith("https://"))) {
      trace("Rsrc", frame, state, url, contentType, progress, errorCode);
    }
  }

  void resetStatusCode() {
    resetStatusCode(true);
  }

  private void resetStatusCode(boolean startNewListener) {
    synchronized (statusCode) {
      for (Thread thread : ajaxListeners) {
        thread.interrupt();
      }
      ajaxListeners.clear();
      statusCode.set(0);
      resources.clear();
      if (startNewListener) {
        Thread thread = new Thread(new AjaxListener(
            200, statusCode, resources, timeoutMS.get()));
        ajaxListeners.add(thread);
        thread.start();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispatchLoadEvent(final long frame, final int state, String url,
      String contentType, double progress, int errorCode) {
    final Settings settings = SettingsManager.settings();
    if (settings == null) {
      throw new RuntimeException("Request made after browser closed. Ignoring...");
    }
    synchronized (statusCode) {
      if (state == LoadListenerClient.PAGE_STARTED) {
        contextItem.resetFrameId(frame);
        if (settings.logJavascript()) {
          JavascriptLog.attach(Accessor.getPageFor(contextItem.engine.get()), frame);
        }
      }
      contextItem.addFrameId(frame);
      if (state == LoadListenerClient.PAGE_STARTED
          || state == LoadListenerClient.PAGE_REDIRECTED
          || state == LoadListenerClient.DOCUMENT_AVAILABLE) {
        if (contextItem.currentFrameId() == frame) {
          if (state == LoadListenerClient.PAGE_STARTED) {
            StatusMonitor.instance().clearStatusMonitor();
          }
          resetStatusCode(false);
          resources.put(frame + url, System.currentTimeMillis());
          statusMonitor.startStatusMonitor(url);
          statusMonitor.addPrimaryDocument(true, url);
        } else {
          statusMonitor.addPrimaryDocument(false, url);
        }
      } else if (statusCode.get() == 0
          && contextItem.currentFrameId() == frame
          && (state == LoadListenerClient.PAGE_FINISHED
              || state == LoadListenerClient.LOAD_STOPPED
              || state == LoadListenerClient.LOAD_FAILED)) {
        final int newStatusCode = statusMonitor.stopStatusMonitor(url);
        resources.remove(frame + url);
        Thread thread = new Thread(new AjaxListener(newStatusCode, statusCode,
            resources, timeoutMS.get()));
        ajaxListeners.add(thread);
        thread.start();
      }
    }
    if (settings.logTrace()) {
      trace("Page", frame, state, url, contentType, progress, errorCode);
    }
  }

}
