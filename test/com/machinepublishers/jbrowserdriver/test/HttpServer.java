package com.machinepublishers.jbrowserdriver.test;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HttpServer {
  private static final AtomicBoolean loop = new AtomicBoolean();
  private static final AtomicReference<Closeable> listener = new AtomicReference<Closeable>();

  public static void launch() {
    if (loop.compareAndSet(false, true)) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try (ServerSocket serverSocket = new ServerSocket(9000)) {
            listener.set(serverSocket);
            while (loop.get()) {
              try (Socket socket = serverSocket.accept();
                  DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                  BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null;) {
                  if (line.startsWith("GET")) {
                    byte[] body = "<html><head></head><body>test</body></html>".getBytes("utf-8");
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
