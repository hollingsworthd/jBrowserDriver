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

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import sun.net.www.MessageHeader;

import com.machinepublishers.jbrowserdriver.Logs;

public class StreamHeader extends MessageHeader {
  private final HttpURLConnection conn;
  private final boolean https;

  public StreamHeader(HttpURLConnection conn, MessageHeader existing, Object instProxyOwner, boolean https) {
    for (int i = 0;; i++) {
      String key = existing.getKey(i);
      String value = existing.getValue(i);
      if (key == null && value == null) {
        break;
      }
      if ("User-Agent".equals(key)) {
        AtomicReference<Settings> settings = SettingsManager.registry(value);
        if (settings != null && !settings.get().proxy().directConnection()) {
          try {
            Field instProxy = sun.net.www.protocol.http.HttpURLConnection.class.getDeclaredField("instProxy");
            instProxy.setAccessible(true);
            instProxy.set(instProxyOwner, new java.net.Proxy(settings.get().proxy().type(),
                new InetSocketAddress(settings.get().proxy().host(), settings.get().proxy().port())));
          } catch (Throwable t) {
            Logs.exception(t);
          }
        }
      }
      super.add(key, value);
    }
    this.conn = conn;
    this.https = https;
  }

  @Override
  public synchronized void print(PrintStream printStream) {
    LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
    for (int i = 1;; i++) {
      String key = super.getKey(i);
      String value = super.getValue(i);
      if (key == null && value == null) {
        break;
      }
      headers.put(key, value);
    }
    headers = SettingsManager.processHeaders(headers, conn, https);
    printStream.print(super.getKey(0)
        + (super.getValue(0) == null ? "" : ": " + super.getValue(0)) + "\r\n");
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      printStream.print(entry.getKey()
          + (entry.getValue() == null ? "" : ": " + entry.getValue()) + "\r\n");
    }
    printStream.print("\r\n");
    printStream.flush();
  }
}
