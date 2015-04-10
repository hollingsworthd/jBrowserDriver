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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;

import com.machinepublishers.browser.Browser;

class Util {
  private static final Pattern charsetPattern = Pattern.compile(
      "charset\\s*=\\s*([^;]+)", Pattern.CASE_INSENSITIVE);
  private static final Random rand = new Random();

  static enum Pause {
    LONG, SHORT, NONE
  }

  static void close(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Throwable t) {
        Logs.exception(t);
      }
    }
  }

  static void close(HttpURLConnection conn) {
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

  static interface Sync<T> {
    T perform();
  }

  private static class Runner<T> implements Runnable {
    private final Sync<T> action;
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicReference<T> returned = new AtomicReference<T>();
    private final AtomicReference<RuntimeException> fatal = new AtomicReference<RuntimeException>();
    private final AtomicReference<RuntimeException> retry = new AtomicReference<RuntimeException>();

    public Runner(Sync<T> action) {
      this.action = action;
    }

    @Override
    public void run() {
      T result = null;
      Browser.Fatal browserFatal = null;
      Browser.Retry browserRetry = null;
      try {
        result = action.perform();
      } catch (Browser.Fatal t) {
        browserFatal = t;
      } catch (Browser.Retry t) {
        browserRetry = t;
      }
      synchronized (done) {
        fatal.set(browserFatal);
        retry.set(browserRetry);
        returned.set(result);
        done.set(true);
        done.notifyAll();
      }
    }
  }

  private static void pause(final Pause pauseLength, final long settingsId) {
    Util.exec(Pause.NONE, new AtomicInteger(-1), new Sync<Object>() {
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
    }, settingsId);
  }

  static <T> T exec(Pause pauseAfterExec, final Sync<T> action, final long id) {
    return exec(pauseAfterExec, new AtomicInteger(-1), 0, action, id);
  }

  static <T> T exec(Pause pauseAfterExec, final long timeout, final Sync<T> action, final long id) {
    return exec(pauseAfterExec, new AtomicInteger(-1), timeout, action, id);
  }

  static <T> T exec(Pause pauseAfterExec, final AtomicInteger statusCode, final Sync<T> action, final long id) {
    return exec(pauseAfterExec, statusCode, 0, action, id);
  }

  static <T> T exec(Pause pauseAfterExec, final AtomicInteger statusCode, final long timeout,
      final Sync<T> action, final long id) {
    if (statusCode.get() != -1) {
      synchronized (statusCode) {
        if (statusCode.get() == 0) {
          try {
            statusCode.wait();
          } catch (Throwable t) {}
        }
      }
      if (Logs.TRACE && statusCode.get() != 200) {
        System.out.println("Performing browser action, but HTTP status is " + statusCode.get() + ".");
      }
    }
    try {
      if ((boolean) JavaFx.getStatic(Platform.class, id).call("isFxApplicationThread").unwrap()) {
        try {
          return action.perform();
        } catch (Browser.Fatal t) {
          throw t;
        } catch (Browser.Retry t) {
          throw t;
        }
      }
      final Runner<T> runner = new Runner<T>(action);
      synchronized (runner.done) {
        JavaFx.getStatic(Platform.class, id).call("runLater", runner);
      }
      synchronized (runner.done) {
        if (!runner.done.get()) {
          try {
            runner.done.wait(timeout);
          } catch (InterruptedException e) {
            Logs.exception(e);
          }
          if (!runner.done.get()) {
            Logs.exception(new RuntimeException("Action never completed."));
          }
        }
        if (runner.fatal.get() != null) {
          throw runner.fatal.get();
        }
        if (runner.retry.get() != null) {
          throw runner.retry.get();
        }
        return runner.returned.get();
      }
    } finally {
      if (pauseAfterExec != Pause.NONE) {
        pause(pauseAfterExec, id);
      }
    }
  }

  static String toString(InputStream inputStream, String charset) {
    try {
      final char[] chars = new char[8192];
      StringBuilder builder = new StringBuilder();
      InputStreamReader reader = new InputStreamReader(inputStream, charset);
      for (int len; -1 != (len = reader.read(chars));) {
        builder.append(chars, 0, len);
      }
      return builder.toString();
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    }
  }

  static byte[] toBytes(InputStream inputStream) throws IOException {
    try {
      final byte[] bytes = new byte[8192];
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      for (int len = 0; -1 != (len = inputStream.read(bytes));) {
        out.write(bytes, 0, len);
      }
      return out.toByteArray();
    } finally {
      close(inputStream);
    }
  }

  static String charset(URLConnection conn) {
    String charset = conn.getContentType();
    if (charset != null) {
      Matcher matcher = charsetPattern.matcher(charset);
      if (matcher.find()) {
        charset = matcher.group(1);
        if (Charset.isSupported(charset)) {
          return charset;
        }
      }
    }
    return "utf-8";
  }
}
