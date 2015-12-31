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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class StreamInjectors {
  public static interface Injector {
    byte[] inject(StreamConnection connection, byte[] inflatedContent, String originalUrl);
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
      String originalUrl) throws IOException {
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
          byte[] newContent = injector.inject(conn, bytes, originalUrl);
          if (newContent != null) {
            bytes = newContent;
          }
        }
      }
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    } finally {
      Util.close(inputStream);
    }
    conn.setContentLength(bytes.length);
    return new ByteArrayInputStream(bytes);
  }
}
