/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

class StreamInjectors {
  public static interface Injector {
    byte[] inject(HttpURLConnection connection, byte[] inflatedContent, String originalUrl, long settingsId);
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

  static InputStream injectedStream(HttpURLConnection conn,
      String originalUrl, long settingsId) throws IOException {
    if (conn.getErrorStream() != null) {
      return conn.getInputStream();
    }
    byte[] connBytes = Util.toBytes(conn.getInputStream());
    if (connBytes.length > 0) {
      try {
        String encoding = conn.getContentEncoding();
        InputStream in = new ByteArrayInputStream(connBytes);
        if ("gzip".equalsIgnoreCase(encoding)) {
          in = new GZIPInputStream(in);
        } else if ("deflate".equalsIgnoreCase(encoding)) {
          in = new InflaterInputStream(in);
        }
        byte[] content = Util.toBytes(in);
        synchronized (lock) {
          for (Injector injector : injectors) {
            byte[] newContent = injector.inject(conn, content, originalUrl, settingsId);
            if (newContent != null) {
              content = newContent;
            }
          }
        }
        if (content != null) {
          ByteArrayOutputStream out = null;
          try {
            if ("gzip".equalsIgnoreCase(encoding)) {
              out = new ByteArrayOutputStream();
              GZIPOutputStream gzip = new GZIPOutputStream(out);
              gzip.write(content);
              Util.close(gzip);
              return new ByteArrayInputStream(out.toByteArray());
            }
            if ("deflate".equalsIgnoreCase(encoding)) {
              out = new ByteArrayOutputStream();
              DeflaterOutputStream deflate = new DeflaterOutputStream(out);
              deflate.write(content);
              Util.close(deflate);
              return new ByteArrayInputStream(out.toByteArray());
            }
          } finally {
            Util.close(out);
          }
          return new ByteArrayInputStream(content);
        }
      } catch (Throwable t) {
        Logs.exception(t);
      }
    }
    return new ByteArrayInputStream(connBytes);
  }
}
