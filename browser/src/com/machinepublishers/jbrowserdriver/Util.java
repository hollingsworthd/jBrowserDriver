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

import java.io.Closeable;
import java.net.HttpURLConnection;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

public class Util {

  static {
    //needed to initialize jfx
    new JFXPanel();
  }

  public static void close(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Throwable t) {
        Logs.exception(t);
      }
    }
  }

  public static void close(HttpURLConnection conn) {
    /*
     * Don't log exceptions here: we assume that we're being overly cautious,
     * trying to close streams that don't exist or are already closed.
     */
    if (conn != null) {
      try {
        conn.disconnect();
      } catch (Throwable t) {}
      try {
        conn.getInputStream().close();
      } catch (Throwable t) {}
      try {
        conn.getOutputStream().close();
      } catch (Throwable t) {}
      try {
        conn.getErrorStream().close();
      } catch (Throwable t) {}
    }
  }

  public static interface Sync<T> {
    T perform();
  }

  public static interface Async {
    void perform();
  }

  public static void exec(final Async action) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        action.perform();
      }
    });
  }

  public static <T> T exec(final Sync<T> action) {
    return exec(0, action);
  }

  public static <T> T exec(final long timeout, final Sync<T> action) {
    if (Platform.isFxApplicationThread()) {
      return action.perform();
    }
    final T[] ret = (T[]) new Object[1];
    final boolean[] lock = new boolean[1];
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        T result = action.perform();
        synchronized (lock) {
          ret[0] = result;
          lock[0] = true;
          lock.notify();
        }
      }
    });
    synchronized (lock) {
      if (!lock[0]) {
        try {
          lock.wait(timeout);
        } catch (InterruptedException e) {
          Logs.exception(e);
        }
        if (!lock[0]) {
          Logs.exception(new RuntimeException("Action never completed."));
        }
      }
    }
    return ret[0];
  }
}
