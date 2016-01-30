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
import java.util.concurrent.atomic.AtomicReference;

public class HttpServer {
  private static final AtomicBoolean loop = new AtomicBoolean();
  private static final AtomicReference<Closeable> listener = new AtomicReference<Closeable>();
  private static final AtomicReference<List<String>> previousRequest = new AtomicReference<List<String>>();
  private static final byte[] body;
  private static final byte[] content;
  static {
    final char[] chars = new char[8192];
    StringBuilder builder = new StringBuilder(chars.length);
    byte[] bodyTmp = null;
    byte[] contentTmp = null;
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(HttpServer.class.getResourceAsStream(
            "/com/machinepublishers/jbrowserdriver/diagnostics/test.htm")))) {
      for (int len; -1 != (len = reader.read(chars, 0, chars.length)); builder.append(chars, 0, len));
      bodyTmp = builder.toString().getBytes("utf-8");
      contentTmp = new String("HTTP/1.1 200 OK\n"
          + "Content-Length: " + bodyTmp.length + "\n"
          + "Content-Type: text/html; charset=utf-8\n"
          + "Connection: close\n\n").getBytes("utf-8");
    } catch (Throwable t) {
      t.printStackTrace();
    }
    body = bodyTmp;
    content = contentTmp;
  }

  public static List<String> previousRequest() {
    return previousRequest.get();
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
                  if (line.startsWith("GET")) {
                    output.write(content, 0, content.length);
                    output.write(body, 0, body.length);
                  }
                }
                previousRequest.set(request);
              }
            }
          } catch (Throwable t) {}
        }
      }).start();
    }
  }

  public static void stop() {
    loop.set(false);
    try {
      listener.get().close();
    } catch (Throwable t) {}
  }
}
