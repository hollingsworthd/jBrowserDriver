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

import javafx.scene.web.WebView;

import com.machinepublishers.jbrowserdriver.config.JavaFx;
import com.machinepublishers.jbrowserdriver.config.StreamHandler;
import com.sun.webkit.LoadListenerClient;

public class DynamicHttpListener implements LoadListenerClient {
  private final WebView view;
  private final AtomicInteger statusCode;
  private final long settingsId;

  public DynamicHttpListener(WebView view, AtomicInteger statusCode, long settingsId) {
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
    if (Logs.TRACE) {
      trace("Rsrc", frame, state, url, contentType, progress, errorCode);
    }
  }

  @Override
  public void dispatchLoadEvent(long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    if (state == LoadListenerClient.PAGE_STARTED) {
      statusCode.set(0);
      JavaFx.getStatic(StreamHandler.class, settingsId).call("startStatusMonitor", view.getEngine().getLocation());
    } else if (state == LoadListenerClient.PAGE_FINISHED) {
      int code = Integer.parseInt(JavaFx.getStatic(StreamHandler.class, settingsId).
          call("stopStatusMonitor", view.getEngine().getLocation()).toString());
      statusCode.set(code);
      synchronized (statusCode) {
        statusCode.notifyAll();
      }
    } else if (state == LoadListenerClient.LOAD_FAILED || state == LoadListenerClient.LOAD_STOPPED) {
      JavaFx.getStatic(StreamHandler.class, settingsId).call("stopStatusMonitor", view.getEngine().getLocation());
      statusCode.set(499);
      synchronized (statusCode) {
        statusCode.notifyAll();
      }
    }
    if (Logs.TRACE) {
      trace("Page", frame, state, url, contentType, progress, errorCode);
    }
  }
}
