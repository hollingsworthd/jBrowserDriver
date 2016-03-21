/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC
 * 
 * Sales and support: ops@machinepublishers.com
 * Updates: https://github.com/MachinePublishers/jBrowserDriver
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

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;

class AppThread {
  private static final Random rand = new Random();

  static enum Pause {
    LONG, SHORT, NONE
  }

  static interface Sync<T> {
    T perform();
  }

  private static class Runner<T> implements Runnable {
    private final Sync<T> action;
    private final AtomicInteger statusCode;
    private final AtomicBoolean done;
    private final AtomicReference<T> returned;

    public Runner(Sync<T> action, AtomicInteger statusCode) {
      this.action = action;
      this.statusCode = statusCode;
      this.done = new AtomicBoolean();
      this.returned = new AtomicReference<T>();
    }

    public Runner(Runner other) {
      this.action = other.action;
      this.statusCode = other.statusCode;
      this.done = other.done;
      this.returned = other.returned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      synchronized (statusCode) {
        if (statusCode.get() == 0) {
          Platform.runLater(new Runner(this));
        } else {
          if (statusCode.get() != -1 && statusCode.get() > 299) {
            LogsServer.instance().trace("Performing browser action, but HTTP status is " + statusCode.get() + ".");
          }
          T result = null;
          try {
            result = action.perform();
          } finally {
            synchronized (done) {
              returned.set(result);
              done.set(true);
              done.notifyAll();
            }
          }
        }
      }
    }
  }

  private static void pause(final Pause pauseLength) {
    AppThread.exec(Pause.NONE, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        try {
          if (pauseLength == Pause.SHORT) {
            Thread.sleep(0, 1);
          } else if (pauseLength == Pause.LONG) {
            Thread.sleep(60 + rand.nextInt(60));
          }
        } catch (Throwable t) {}
        return null;
      }
    });
  }

  static <T> T exec(Pause pauseAfterExec, final AtomicInteger statusCode, final Sync<T> action) {
    return exec(pauseAfterExec, statusCode, 0, action);
  }

  static <T> T exec(Pause pauseAfterExec, final AtomicInteger statusCode, final long timeout,
      final Sync<T> action) {
    try {
      if ((boolean) Platform.isFxApplicationThread()) {
        return action.perform();
      }
      final Runner<T> runner = new Runner<T>(action, statusCode);
      synchronized (runner.done) {
        Platform.runLater(runner);
      }
      synchronized (runner.done) {
        if (!runner.done.get()) {
          try {
            runner.done.wait(timeout);
          } catch (InterruptedException e) {
            LogsServer.instance().exception(e);
          }
          if (!runner.done.get()) {
            LogsServer.instance().exception(new RuntimeException("Action never completed."));
          }
        }
        return runner.returned.get();
      }
    } finally {
      if (pauseAfterExec != Pause.NONE) {
        pause(pauseAfterExec);
      }
    }
  }
}
