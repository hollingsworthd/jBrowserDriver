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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class StreamInterceptors {
  private static final Object lock = new Object();
  private static final List<ResponseInterceptor> responseInterceptors = new ArrayList<ResponseInterceptor>();

  static void add(ResponseInterceptor responseInterceptor) {
    synchronized (lock) {
      responseInterceptors.add(responseInterceptor);
    }
  }

  static void remove(ResponseInterceptor responseInterceptor) {
    synchronized (lock) {
      responseInterceptors.remove(responseInterceptor);
    }
  }

  static void removeAll() {
    synchronized (lock) {
      responseInterceptors.clear();
    }
  }

  static InputStream interceptedResponse(StreamConnection conn, InputStream inputStream,
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
        for (ResponseInterceptor responseInterceptor : responseInterceptors) {
          conn.setContentLength(bytes.length);
          byte[] newContent = responseInterceptor.intercept(conn, bytes, originalUrl);
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

  static void init() {
    final Pattern head = Pattern.compile("<head\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Pattern html = Pattern.compile("<html\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Pattern body = Pattern.compile("<body\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Set<Integer> redirectCodes = Collections.unmodifiableSet(
        new HashSet<Integer>(Arrays.asList(new Integer[] { 301, 302, 303, 307, 308 })));
    StreamInterceptors.add(new ResponseInterceptor() {
      @Override
      public byte[] intercept(HttpURLConnection connection,
          byte[] inflatedContent, String originalUrl) {
        final Settings settings = SettingsManager.settings();
        try {
          if (settings.saveMedia()
              && ((StreamConnection) connection).isMedia()) {
            String filename = Long.toString(System.nanoTime());
            File contentFile = new File(StreamConnection.mediaDir(),
                new StringBuilder().append(filename).append(".content").toString());
            File metaFile = new File(StreamConnection.mediaDir(),
                new StringBuilder().append(filename).append(".metadata").toString());
            while (contentFile.exists() || metaFile.exists()) {
              filename = Util.randomFileName();
              contentFile = new File(StreamConnection.mediaDir(),
                  new StringBuilder().append(filename).append(".content").toString());
              metaFile = new File(StreamConnection.mediaDir(),
                  new StringBuilder().append(filename).append(".metadata").toString());
            }
            contentFile.deleteOnExit();
            metaFile.deleteOnExit();
            Files.write(contentFile.toPath(), inflatedContent);
            Files.write(metaFile.toPath(),
                (new StringBuilder().append(originalUrl).append("\n").append(connection.getContentType())
                    .toString()).getBytes("utf-8"));
          }
        } catch (Throwable t) {}
        try {
          if (settings.quickRender() && ((StreamConnection) connection).isMedia()) {
            LogsServer.instance().trace("Media discarded: " + connection.getURL().toExternalForm());
            StatusMonitor.instance().addDiscarded(connection.getURL().toExternalForm());
            return new byte[0];
          } else if (!redirectCodes.contains(connection.getResponseCode())
              && (connection.getContentType() == null || connection.getContentType().indexOf("text/html") > -1)
              && StatusMonitor.instance().isPrimaryDocument(connection.getURL().toExternalForm())) {
            String intercepted = null;
            String charset = Util.charset(connection);
            String content = new String(inflatedContent, charset);
            Matcher matcher = head.matcher(content);
            if (matcher.find()) {
              intercepted = matcher.replaceFirst(matcher.group(0) + settings.script());
            } else {
              matcher = html.matcher(content);
              if (matcher.find()) {
                intercepted = matcher.replaceFirst(new StringBuilder()
                    .append(matcher.group(0))
                    .append("<head>")
                    .append(settings.script())
                    .append("</head>").toString());
              } else {
                matcher = body.matcher(content);
                if (matcher.find()) {
                  intercepted = (new StringBuilder().append("<html><head>").append(settings.script())
                      .append("</head>").append(content).append("</html>").toString());
                } else {
                  intercepted = content;
                }
              }
            }
            return intercepted == null ? null : intercepted.getBytes(charset);
          }
        } catch (Throwable t) {}
        return null;
      }
    });
  }
}
