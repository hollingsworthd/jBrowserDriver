/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;

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
      } catch (Throwable t) {}
    }
  }

  static interface Sync<T> {
    T perform();
  }

  private static class Runner<T> implements Runnable {
    private final Sync<T> action;
    private final AtomicInteger statusCode;
    private final long id;
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicReference<T> returned = new AtomicReference<T>();
    private final AtomicReference<RuntimeException> fatal = new AtomicReference<RuntimeException>();
    private final AtomicReference<RuntimeException> retry = new AtomicReference<RuntimeException>();

    public Runner(Sync<T> action, AtomicInteger statusCode, long id) {
      this.action = action;
      this.statusCode = statusCode;
      this.id = id;
    }

    @Override
    public void run() {
      if (statusCode.get() != -1) {
        if (statusCode.get() == 0) {
          Platform.runLater(this);
          return;
        }
        if (statusCode.get() != 200) {
          Logs.logsFor(id).trace("Performing browser action, but HTTP status is " + statusCode.get() + ".");
        }
      }
      T result = null;
      BrowserException.Fatal browserFatal = null;
      BrowserException.Retry browserRetry = null;
      try {
        result = action.perform();
      } catch (BrowserException.Fatal t) {
        browserFatal = t;
      } catch (BrowserException.Retry t) {
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

  static <T> T exec(Pause pauseAfterExec, final AtomicInteger statusCode, final Sync<T> action, final long id) {
    return exec(pauseAfterExec, statusCode, 0, action, id);
  }

  static <T> T exec(Pause pauseAfterExec, final AtomicInteger statusCode, final long timeout,
      final Sync<T> action, final long id) {
    try {
      if ((boolean) Platform.isFxApplicationThread()) {
        try {
          return action.perform();
        } catch (BrowserException.Fatal t) {
          throw t;
        } catch (BrowserException.Retry t) {
          throw t;
        }
      }
      final Runner<T> runner = new Runner<T>(action, statusCode, id);
      synchronized (runner.done) {
        Platform.runLater(runner);
      }
      synchronized (runner.done) {
        if (!runner.done.get()) {
          try {
            runner.done.wait(timeout);
          } catch (InterruptedException e) {
            Logs.logsFor(id).exception(e);
          }
          if (!runner.done.get()) {
            Logs.logsFor(id).exception(new RuntimeException("Action never completed."));
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
      StringBuilder builder = new StringBuilder(chars.length);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset), chars.length);
      try {
        for (int len; -1 != (len = reader.read(chars, 0, chars.length)); builder.append(chars, 0, len));
      } catch (EOFException e) {}
      return builder.toString();
    } catch (Throwable t) {
      return null;
    } finally {
      close(inputStream);
    }
  }

  static byte[] toBytes(InputStream inputStream) throws IOException {
    try {
      final byte[] bytes = new byte[8192];
      ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
      try {
        for (int len = 0; -1 != (len = inputStream.read(bytes, 0, bytes.length)); out.write(bytes, 0, len));
      } catch (EOFException e) {}
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
