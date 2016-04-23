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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;

class SocketFactory extends RMISocketFactory implements Serializable {
  private final InetAddress host;
  private final int parentPort;
  private final int childPort;
  private transient Socket clientSocket = new Socket();
  private final SocketLock lock;

  SocketFactory(String host, int parentPort, int childPort, SocketLock lock) {
    InetAddress hostTmp = null;
    try {
      hostTmp = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      Util.handleException(e);
    }
    this.host = hostTmp;
    this.parentPort = parentPort;
    this.childPort = childPort;
    this.lock = lock;
  }

  @Override
  public ServerSocket createServerSocket(int p) throws IOException {
    ServerSocket serverSocket = new ServerSocket();
    serverSocket.setReuseAddress(true);
    serverSocket.bind(new InetSocketAddress(host, childPort), Integer.MAX_VALUE);
    return serverSocket;
  }

  @Override
  public Socket createSocket(String h, int p) throws IOException {
    synchronized (lock) {
      final int retries = 5;
      for (int i = 1; i <= retries; i++) {
        Socket prevClientSocket = clientSocket;
        try {
          clientSocket = new Socket();
          clientSocket.setReuseAddress(true);
          clientSocket.setTcpNoDelay(true);
          clientSocket.setKeepAlive(true);
          //TODO for binding, require parent port and daemon port for each process
          clientSocket.bind(new InetSocketAddress(host, parentPort));
          clientSocket.connect(new InetSocketAddress(host, childPort));
          return clientSocket;
        } catch (IOException e) {
          if (i == retries) {
            throw e;
          }
          try {
            Thread.sleep(50);
          } catch (InterruptedException e2) {}
          prevClientSocket.close();
          clientSocket.close();
        }
      }
    }
    throw new IOException();
  }

  @Override
  public int hashCode() {
    return SocketFactory.class.getName().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SocketFactory;
  }
}
