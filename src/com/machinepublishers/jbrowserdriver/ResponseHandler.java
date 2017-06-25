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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.lang.StringUtils;

class ResponseHandler {
  private static final Pattern head = Pattern.compile("<head\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern html = Pattern.compile("<html\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern body = Pattern.compile("<body\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Set<Integer> redirectCodes = Collections.unmodifiableSet(
      new HashSet<Integer>(Arrays.asList(new Integer[] { 301, 302, 303, 307, 308 })));

  static InputStream handleResponse(StreamConnection conn, InputStream inputStream) throws IOException {
    String url = conn.getURL().toExternalForm();
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
      conn.setContentLength(bytes.length);

      Settings settings = SettingsManager.settings();
      if (settings != null) {
        String disposition = conn.getHeaderField("Content-Disposition");

        if (settings.saveAttachments() && disposition != null
            && StatusMonitor.instance().isPrimaryDocument(true, url)) {
          writeContentToDisk(bytes, StreamConnection.attachmentsDir(), url, conn.getContentTypeRaw(), disposition);
        }

        if (settings.saveMedia() && ((StreamConnection) conn).isMedia()) {
          writeContentToDisk(bytes, StreamConnection.mediaDir(), url, conn.getContentTypeRaw(), disposition);
        }
      }
      byte[] newContent = getBody(conn, bytes, url);
      if (newContent != null) {
        bytes = newContent;
      }
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    } finally {
      Util.close(inputStream);
    }
    conn.setContentLength(bytes.length);
    return new ByteArrayInputStream(bytes);
  }

  private static void writeContentToDisk(byte[] content, File dir, String url, String contentType, String contentDisposition) {
    String filename = Util.randomFileName();

    File contentFile = new File(dir,
        new StringBuilder().append(filename).append(".content").toString());

    File metaFile = new File(dir,
        new StringBuilder().append(filename).append(".metadata").toString());

    contentFile.deleteOnExit();
    metaFile.deleteOnExit();

    try {
      Files.write(contentFile.toPath(), content);
      Files.write(metaFile.toPath(),
          (new StringBuilder()
              .append(StringUtils.isEmpty(url) ? "" : url).append("\n")
              .append(StringUtils.isEmpty(contentType) ? "" : contentType).append("\n")
              .append(StringUtils.isEmpty(contentDisposition) ? "" : contentDisposition)
              .toString()).getBytes("utf-8"));
    } catch (Throwable t) {}
  }

  private static byte[] getBody(StreamConnection connection, byte[] inflatedContent, String url) {
    final Settings settings = SettingsManager.settings();
    try {
      if (settings.quickRender() && ((StreamConnection) connection).isMedia()) {
        LogsServer.instance().trace("Media discarded: " + url);
        StatusMonitor.instance().addDiscarded(url);
        return new byte[0];
      } else if (!redirectCodes.contains(connection.getResponseCode())
          && (connection.getContentType() == null || connection.getContentType().indexOf("text/html") > -1)
          && StatusMonitor.instance().isPrimaryDocument(false, url)) {
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
}
