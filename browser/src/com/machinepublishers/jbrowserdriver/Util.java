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

import com.machinepublishers.jbrowserdriver.config.JavaFx;

public class Util {
  private static final Pattern charsetPattern = Pattern.compile(
      "charset\\s*=\\s*([^;]+)", Pattern.CASE_INSENSITIVE);
  private static final Random rand = new Random();

  public static enum Pause {
    LONG, SHORT, NONE
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

  private static class Runner<T> implements Runnable {
    private final Sync<T> action;
    private final AtomicBoolean done;
    private final AtomicReference<T> ret;

    public Runner(Sync<T> action, AtomicBoolean done, AtomicReference<T> ret) {
      this.action = action;
      this.done = done;
      this.ret = ret;
    }

    @Override
    public void run() {
      T result = action.perform();
      synchronized (done) {
        ret.set(result);
        done.set(true);
        done.notify();
      }
    }
  }

  private static void pause(Pause pauseLength, final long settingsId) {
    Util.exec(Pause.NONE, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        try {
          if (pauseLength == Pause.SHORT) {
            Thread.sleep(0, 1);
          } else if (pauseLength == Pause.LONG) {
            Thread.sleep(70 + rand.nextInt(70));
          }
        } catch (Throwable t) {}
        return null;
      }
    }, settingsId);
  }

  public static <T> T exec(Pause pauseAfterExec, final Sync<T> action, final long id) {
    return exec(pauseAfterExec, new AtomicInteger(-1), 0, action, id);
  }

  public static <T> T exec(Pause pauseAfterExec, final AtomicInteger statusCode, final Sync<T> action, final long id) {
    return exec(pauseAfterExec, statusCode, 0, action, id);
  }

  public static <T> T exec(Pause pauseAfterExec, final AtomicInteger statusCode, final long timeout,
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
        return action.perform();
      }
      final AtomicReference<T> ret = new AtomicReference<T>();
      final AtomicBoolean done = new AtomicBoolean();
      synchronized (done) {
        JavaFx.getStatic(Platform.class, id).call("runLater", new Runner<T>(action, done, ret));
      }
      synchronized (done) {
        if (!done.get()) {
          try {
            done.wait(timeout);
          } catch (InterruptedException e) {
            Logs.exception(e);
          }
          if (!done.get()) {
            Logs.exception(new RuntimeException("Action never completed."));
          }
        }
        return ret.get();
      }
    } finally {
      if (pauseAfterExec != Pause.NONE) {
        pause(pauseAfterExec, id);
      }
    }
  }

  public static String toString(InputStream inputStream, String charset) {
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

  public static byte[] toBytes(InputStream inputStream) throws IOException {
    final byte[] bytes = new byte[8192];
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int len = 0; -1 != (len = inputStream.read(bytes));) {
      out.write(bytes, 0, len);
    }
    return out.toByteArray();
  }

  public static String charset(URLConnection conn) {
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
