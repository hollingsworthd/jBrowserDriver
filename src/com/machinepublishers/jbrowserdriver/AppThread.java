/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
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

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.TimeoutException;

import com.google.common.util.concurrent.UncheckedExecutionException;

import javafx.application.Platform;

class AppThread {
  private static final Random rand = new Random();

  static interface Sync<T> {
    T perform();
  }

  private static class Runner<T> implements Runnable {
    private static final int MAX_DELAY = 500;
    private final Sync<T> action;
    private final StatusCode statusCode;
    private final AtomicBoolean done;
    private final AtomicReference<T> returned;
    private final int delay;
    private final AtomicBoolean cancel;
    private final AtomicReference<Throwable> failure;

    public Runner(Sync<T> action, StatusCode statusCode) {
      this.action = action;
      this.statusCode = statusCode;
      this.done = new AtomicBoolean();
      this.returned = new AtomicReference<T>();
      this.delay = 1;
      this.cancel = new AtomicBoolean();
      this.failure = new AtomicReference<Throwable>();
    }

    public Runner(Runner other) {
      this.action = other.action;
      this.statusCode = other.statusCode;
      this.done = other.done;
      this.returned = other.returned;
      this.delay = Math.min(MAX_DELAY, other.delay * 2);
      this.cancel = other.cancel;
      this.failure = other.failure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      if (!cancel.get()) {
        synchronized (statusCode) {
          if (statusCode.get() == 0) {
            final Runner newRunner = new Runner(this);
            new Thread(new Runnable() {
              @Override
              public void run() {
                try {
                  Thread.sleep(delay);
                } catch (InterruptedException e) {}
                Platform.runLater(newRunner);
              }
            }).start();
          } else {
            if (statusCode.get() > 299) {
              LogsServer.instance().trace("Performing browser action, but HTTP status is " + statusCode.get() + ".");
            }
            T result = null;
            try {
              result = action.perform();
            } catch (Throwable t) {
              failure.set(t);
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
  }

  private static void pause() {
    AppThread.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        try {
          Thread.sleep(30 + rand.nextInt(40));
        } catch (Throwable t) {}
        return null;
      }
    });
  }

  static void handleExecutionException(Object obj) {
    if (obj instanceof UncheckedExecutionException) {
      throw (UncheckedExecutionException) obj;
    }
    if (obj instanceof RuntimeException) {
      throw new UncheckedExecutionException((RuntimeException) obj);
    }
    if (obj instanceof Throwable) {
      throw new UncheckedExecutionException(new RuntimeException((Throwable) obj));
    }
  }

  static <T> T exec(final Sync<T> action) {
    return exec(false, new StatusCode(), 0, action);
  }

  static <T> T exec(final Sync<T> action, long timeout) {
    return exec(false, new StatusCode(), timeout, action);
  }

  static <T> T exec(final StatusCode statusCode, final Sync<T> action) {
    return exec(false, statusCode, 0, action);
  }

  static <T> T exec(final boolean pauseAfterExec, final StatusCode statusCode, final Sync<T> action) {
    return exec(pauseAfterExec, statusCode, 0, action);
  }

  static <T> T exec(final StatusCode statusCode, final long timeout, final Sync<T> action) {
    return exec(false, statusCode, timeout, action);
  }

  static <T> T exec(final boolean pauseAfterExec, final StatusCode statusCode, final long timeout,
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
            runner.cancel.set(true);
            throw new TimeoutException(new StringBuilder()
                .append("Timeout of ").append(timeout).append("ms reached.").toString());
          }
        }
        handleExecutionException(runner.failure.get());
        return runner.returned.get();
      }
    } finally {
      if (pauseAfterExec) {
        pause();
      }
    }
  }
}
