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
package com.machinepublishers.jbrowserdriver.diagnostics;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class HttpServer {
  private static final AtomicBoolean loop = new AtomicBoolean();
  private static final AtomicReference<Closeable> listener = new AtomicReference<Closeable>();
  private static final AtomicReference<List<String>> previousRequest = new AtomicReference<List<String>>();
  private static final AtomicLong previousRequestId = new AtomicLong();
  private static final byte[] indexBody;
  private static final byte[] indexContent;
  private static final byte[] iframeBody;
  private static final byte[] iframeContent;
  private static final byte[] redirectContent;
  static {
    byte[][] resource = resource("/com/machinepublishers/jbrowserdriver/diagnostics/test.htm");
    indexBody = resource[0];
    indexContent = resource[1];

    resource = resource("/com/machinepublishers/jbrowserdriver/diagnostics/iframe.htm");
    iframeBody = resource[0];
    iframeContent = resource[1];

    byte[] redirectContentTmp = null;
    try {
      redirectContentTmp = new String(""
          + "HTTP/1.1 302 Found\n"
          + "Server: Initech/1.1\n"
          + "X-FRAME-OPTIONS: SAMEORIGIN\n"
          + "Set-Cookie: JSESSIONID=ABC123; Path=/redirect/; HttpOnly\n"
          + "Expires: 0\n"
          + "Cache-Control: no-store, no-cache\n"
          + "Cache-Control: post-check=0, pre-check=0\n"
          + "Pragma: no-cache\n"
          + "Location: /redirect/site2\n"
          + "Content-Type: text/html;charset=UTF-8\n"
          + "Content-Length: 0\n\n").getBytes("utf-8");
    } catch (Throwable t) {
      t.printStackTrace();
    }
    redirectContent = redirectContentTmp;
  }

  private static byte[][] resource(String path) {
    final char[] chars = new char[8192];
    StringBuilder builder = new StringBuilder(chars.length);
    byte[] bodyTmp = null;
    byte[] contentTmp = null;
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(HttpServer.class.getResourceAsStream(path)))) {
      for (int len; -1 != (len = reader.read(chars, 0, chars.length)); builder.append(chars, 0, len));
      bodyTmp = builder.toString().getBytes("utf-8");
      contentTmp = new String("HTTP/1.1 200 OK\n"
          + "Content-Length: " + bodyTmp.length + "\n"
          + "Content-Type: text/html; charset=utf-8\n"
          + "Expires: Sun, 09 Feb 2116 01:01:01 GMT\n"
          + "Connection: close\n\n").getBytes("utf-8");
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return new byte[][] { bodyTmp, contentTmp };
  }

  public static List<String> previousRequest() {
    return previousRequest.get();
  }

  public static long previousRequestId() {
    return previousRequestId.get();
  }

  public static void launch(int port) {
    if (loop.compareAndSet(false, true)) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getLoopbackAddress())) {
            listener.set(serverSocket);
            while (loop.get()) {
              try (Socket socket = serverSocket.accept();
                  DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                  BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                List<String> request = new ArrayList<String>();
                for (String line; (line = reader.readLine()) != null;) {
                  request.add(line);
                  if (line.startsWith("GET / ")) {
                    output.write(indexContent, 0, indexContent.length);
                    output.write(indexBody, 0, indexBody.length);
                  } else if (line.startsWith("GET /iframe.htm")) {
                    output.write(iframeContent, 0, iframeContent.length);
                    output.write(iframeBody, 0, iframeBody.length);
                  } else if (line.startsWith("GET /redirect/site1 ")) {
                    output.write(redirectContent);
                  } else if (line.startsWith("GET /redirect/site2 ")) {
                    output.write(iframeContent, 0, iframeContent.length);
                    output.write(iframeBody, 0, iframeBody.length);
                  } else if (line.startsWith("GET /wait-forever ")) {
                    synchronized (HttpServer.class) {
                      HttpServer.class.wait();
                    }
                  }
                }
                previousRequest.set(request);
                previousRequestId.incrementAndGet();
              }
            }
          } catch (Throwable t) {}
        }
      }).start();
    }
  }

  public static void stop() {
    loop.set(false);
    synchronized (HttpServer.class) {
      HttpServer.class.notifyAll();
    }
    try {
      listener.get().close();
    } catch (Throwable t) {}
  }
}
