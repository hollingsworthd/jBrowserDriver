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
package com.machinepublishers.jbrowserdriver.config;

import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;

public class DynamicPageLoadListener implements ChangeListener<Worker.State> {
  private final WebView view;
  private final AtomicInteger statusCode;
  private final long settingsId;

  public DynamicPageLoadListener(WebView view, AtomicInteger statusCode, long settingsId) {
    this.view = view;
    this.statusCode = statusCode;
    this.settingsId = settingsId;
  }

  @Override
  public void changed(final ObservableValue<? extends Worker.State> observable,
      final Worker.State oldValue, final Worker.State newValue) {
    if (Worker.State.SCHEDULED.equals(newValue)) {
      view.setVisible(false);
      JavaFx.getStatic(StreamHandler.class, settingsId).call("startStatusMonitor");
    } else if (Worker.State.SUCCEEDED.equals(newValue)
        || Worker.State.CANCELLED.equals(newValue)
        || Worker.State.FAILED.equals(newValue)) {
      int code = Integer.parseInt(JavaFx.getStatic(StreamHandler.class, settingsId).
          call("stopStatusMonitor", view.getEngine().getLocation()).toString());
      view.setVisible(true);
      statusCode.set(Worker.State.SUCCEEDED.equals(newValue) ? code : 499);
    }
  }
}