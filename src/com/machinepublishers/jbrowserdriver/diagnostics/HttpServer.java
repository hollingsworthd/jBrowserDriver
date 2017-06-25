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
package com.machinepublishers.jbrowserdriver.diagnostics;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;

public class HttpServer {
  private static final AtomicBoolean loop = new AtomicBoolean();
  private static final AtomicReference<Closeable> listener = new AtomicReference<Closeable>();
  private static final AtomicReference<List<String>> previousRequest = new AtomicReference<List<String>>();
  private static final AtomicLong previousRequestId = new AtomicLong();
  private static final byte[] indexBody;
  private static final byte[] indexContent;
  private static final byte[] postBody;
  private static final byte[] postContent;
  private static final byte[] iframeBody;
  private static final byte[] iframeContent;
  private static final byte[] redirectContent;
  private static final byte[] imageBody;
  private static final byte[] imageContent;
  static {
    byte[][] resource = resource("/com/machinepublishers/jbrowserdriver/diagnostics/test.htm", "200 OK", null);
    indexBody = resource[0];
    indexContent = resource[1];

    resource = resource("/com/machinepublishers/jbrowserdriver/diagnostics/test.htm", "201 Created", null);
    postBody = resource[0];
    postContent = resource[1];

    resource = resource("/com/machinepublishers/jbrowserdriver/diagnostics/iframe.htm", "200 OK", null);
    iframeBody = resource[0];
    iframeContent = resource[1];

    resource = resource("/com/machinepublishers/jbrowserdriver/diagnostics/image.png", "200 OK", "image/png");
    imageBody = resource[0];
    imageContent = resource[1];

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

  private static byte[][] resource(String path, String status, String contentType) {
    byte[] bodyTmp = null;
    byte[] contentTmp = null;
    try (InputStream inputStream = HttpServer.class.getResourceAsStream(path)) {
      bodyTmp = IOUtils.toByteArray(inputStream);
      contentTmp = new String("HTTP/1.1 " + status + "\n"
          + "Content-Length: " + bodyTmp.length + "\n"
          //Don't set content-type for text/html -- test that it's added automatically
          + (contentType == null ? "" : "Content-Type: " + contentType + "\n")
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
                  } else if (line.startsWith("POST / ")) {
                    output.write(postContent, 0, postContent.length);
                    output.write(postBody, 0, postBody.length);
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
                  } else if (line.startsWith("GET /image.png")) {
                    output.write(imageContent, 0, imageContent.length);
                    output.write(imageBody, 0, imageBody.length);
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
