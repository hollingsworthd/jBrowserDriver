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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HttpServer {
  private static final AtomicBoolean loop = new AtomicBoolean();
  private static final AtomicReference<Closeable> listener = new AtomicReference<Closeable>();

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
                for (String line; (line = reader.readLine()) != null;) {
                  if (line.startsWith("GET")) {
                    byte[] body = new String("<html><head></head><body>"
                        + "<div id=\"divtext\" name=\"divs\">test1</div>"
                        + "<div id=\"divtext\" name=\"divs\">test2</div>"
                        + "</body></html>").getBytes("utf-8");
                    byte[] content = new String("HTTP/1.1 200 OK\n"
                        + "Content-Length: " + body.length + "\n"
                        + "Content-Type: text/html; charset=utf-8\n"
                        + "Connection: close\n\n").getBytes("utf-8");
                    output.write(content, 0, content.length);
                    output.write(body, 0, body.length);
                  }
                }
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
