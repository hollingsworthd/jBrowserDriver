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
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

import sun.net.www.MessageHeader;

public class StreamHeader extends MessageHeader {

  private final HttpURLConnection conn;
  private final boolean https;

  public StreamHeader(HttpURLConnection conn, boolean https) {
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
