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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class StreamInjectors {
  public static interface Injector {
    byte[] inject(StreamConnection connection, byte[] inflatedContent, String originalUrl, long settingsId);
  }

  private static final Object lock = new Object();
  private static final List<Injector> injectors = new ArrayList<Injector>();

  static void add(Injector injector) {
    synchronized (lock) {
      injectors.add(injector);
    }
  }

  static void remove(Injector injector) {
    synchronized (lock) {
      injectors.remove(injector);
    }
  }

  static void removeAll() {
    synchronized (lock) {
      injectors.clear();
    }
  }

  static InputStream injectedStream(StreamConnection conn, InputStream inputStream,
      String originalUrl, long settingsId) throws IOException {
    byte[] bytes = new byte[0];
    try {
      if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
        bytes = Util.toBytes(new GZIPInputStream(inputStream));
      } else if ("deflate".equalsIgnoreCase(conn.getContentEncoding())) {
        bytes = Util.toBytes(new InflaterInputStream(inputStream));
      } else {
        bytes = Util.toBytes(inputStream);
      }
      conn.removeContentEncoding();
      synchronized (lock) {
        for (Injector injector : injectors) {
          conn.setContentLength(bytes.length);
          byte[] newContent = injector.inject(conn, bytes, originalUrl, settingsId);
          if (newContent != null) {
            bytes = newContent;
          }
        }
      }
    } catch (Throwable t) {
      Logs.logsFor(settingsId).exception(t);
    } finally {
      Util.close(inputStream);
    }
    conn.setContentLength(bytes.length);
    return new ByteArrayInputStream(bytes);
  }
}
