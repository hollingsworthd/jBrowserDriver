package com.machinepublishers.testserver;

import java.io.IOException;
import java.net.ServerSocket;

public class PortUtil {
  public static Integer findRandomOpenPortOnAllLocalInterfaces() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
