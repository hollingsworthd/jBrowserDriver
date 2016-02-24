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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLProtocolException;

import org.apache.http.ConnectionClosedException;

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
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicReference<T> returned = new AtomicReference<T>();

    public Runner(Sync<T> action, AtomicInteger statusCode) {
      this.action = action;
      this.statusCode = statusCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      if (statusCode.get() != -1) {
        if (statusCode.get() == 0) {
          Platform.runLater(this);
          return;
        }
        if (statusCode.get() > 299) {
          LogsServer.instance().trace("Performing browser action, but HTTP status is " + statusCode.get() + ".");
        }
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

  private static void pause(final Pause pauseLength) {
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

  static String toString(InputStream inputStream, String charset) {
    try {
      final char[] chars = new char[8192];
      StringBuilder builder = new StringBuilder(chars.length);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset), chars.length);
      try {
        for (int len; -1 != (len = reader.read(chars, 0, chars.length)); builder.append(chars, 0, len));
      } catch (EOFException | SSLProtocolException | ConnectionClosedException | SocketException e) {}
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
      } catch (EOFException | SSLProtocolException | ConnectionClosedException | SocketException e) {}
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
