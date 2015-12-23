package com.machinepublishers.jbrowserdriver.test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
  public static void launch() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try (ServerSocket serverSocket = new ServerSocket(9000)) {
          while (true) {
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
