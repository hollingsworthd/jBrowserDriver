/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * jBrowserDriver is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking jBrowserDriver statically or dynamically with other modules is making a combined work
 *   based on jBrowserDriver. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of jBrowserDriver with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on jBrowserDriver. If you modify jBrowserDriver, you may not
 *   extend this exception to your modified version of jBrowserDriver.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import sun.net.www.MessageHeader;

class StreamHeader extends MessageHeader {
  private final StreamConnection conn;
  private final boolean https;
  private final AtomicLong settingsId = new AtomicLong();
  private final AtomicReference<Settings> settings = new AtomicReference<Settings>();

  StreamHeader(StreamConnection conn, MessageHeader existing, Object instProxyOwner, boolean https) {
    for (int i = 0;; i++) {
      String key = existing.getKey(i);
      String value = existing.getValue(i);
      if (key == null && value == null) {
        break;
      }
      if ("User-Agent".equals(key)) {
        settingsId.set(Long.parseLong(value));
        settings.set(SettingsManager.get(settingsId.get()).get());
        StatusMonitor.get(settingsId.get()).addStatusMonitor(conn.getURL(), conn);
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

  AtomicLong settingsId() {
    return settingsId;
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
    headers = processHeaders(headers, conn, https);
    printStream.print(super.getKey(0)
        + (super.getValue(0) == null ? "" : ": " + super.getValue(0)) + "\r\n");
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      printStream.print(entry.getKey()
          + (entry.getValue() == null ? "" : ": " + entry.getValue()) + "\r\n");
    }
    printStream.print("\r\n");
    printStream.flush();
  }

  private LinkedHashMap<String, String> processHeaders(
      LinkedHashMap<String, String> headers, StreamConnection conn, boolean https) {
    LinkedHashMap<String, String> headersIn = new LinkedHashMap<String, String>(headers);
    LinkedHashMap<String, String> headersOut = new LinkedHashMap<String, String>();
    Collection<String> names = https ? settings.get().headers().namesHttps()
        : settings.get().headers().namesHttp();
    for (String name : names) {
      String valueIn = headersIn.remove(name);
      String valueSettings = https ? settings.get().headers().headerHttps(name)
          : settings.get().headers().headerHttp(name);
      if (valueSettings == RequestHeaders.DROP_HEADER) {
        continue;
      }
      if (valueSettings == RequestHeaders.DYNAMIC_HEADER) {
        if (valueIn != null && !valueIn.isEmpty()) {
          headersOut.put(name, valueIn);
        }
      } else {
        headersOut.put(name, valueSettings);
      }
    }
    if ("no-cache".equals(headersIn.get("Cache-Control"))) {
      //JavaFX initially sets Cache-Control to no-cache but real browsers don't 
      headersIn.remove("Cache-Control");
    }
    headersOut.putAll(headersIn);
    return headersOut;
  }
}
