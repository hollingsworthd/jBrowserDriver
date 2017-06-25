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
import java.rmi.RemoteException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLProtocolException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.ConnectionClosedException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.internal.WrapsElement;

import com.google.common.util.concurrent.UncheckedExecutionException;

class Util {
  private static final Pattern charsetPattern = Pattern.compile(
      "charset\\s*=\\s*([^;]+)", Pattern.CASE_INSENSITIVE);
  private static final Random secureRand = new Random();
  static final String KEYBOARD_DELETE = "jbrowserdriver-keyboard-delete";

  static String randomPropertyName() {
    return new StringBuilder()
        .append(RandomStringUtils.randomAlphabetic(1))
        .append(randomAlphanumeric())
        .toString();
  }

  static String randomFileName() {
    return randomAlphanumeric();
  }

  private static String randomAlphanumeric() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      builder.append(Long.toString(Math.abs(secureRand.nextInt()), Math.min(36, Character.MAX_RADIX)));
    }
    return builder.toString();
  }

  static void close(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Throwable t) {}
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

  static void handleException(Throwable throwable) {
    if (throwable != null) {
      String message = throwable.getMessage();
      if ((throwable instanceof UncheckedExecutionException || throwable instanceof RemoteException)
          && throwable.getCause() != null) {
        throwable = throwable.getCause();
        message = throwable.getMessage();
      }
      if (throwable instanceof WebDriverException && throwable instanceof RuntimeException) {
        //Wrap the exception to ensure complete/helpful stack trace info and also preserve the original subtype
        try {
          throwable = throwable.getClass().getConstructor(String.class, Throwable.class).newInstance(message, throwable);
        } catch (Throwable t) {
          try {
            throwable = throwable.getClass().getConstructor(Throwable.class).newInstance(throwable);
          } catch (Throwable t2) {}
        }
        throw (RuntimeException) throwable;
      }
      throw new WebDriverException(message, throwable);
    }
  }

  static Object unwrap(Object element) {
    return element instanceof WrapsElement ? unwrap(((WrapsElement) element).getWrappedElement()) : element;
  }
}
