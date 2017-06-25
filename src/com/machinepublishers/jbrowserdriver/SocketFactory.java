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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMISocketFactory;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

class SocketFactory extends RMISocketFactory implements Serializable {
  private final InetAddress host;
  private final int childPort;
  private final int parentPort;
  private final int parentAltPort;
  private final Set<SocketLock> locks;
  private transient final AtomicReference<Socket> clientSocket = new AtomicReference<Socket>(new Socket());
  private transient final AtomicReference<Socket> clientAltSocket = new AtomicReference<Socket>(new Socket());

  SocketFactory(String host, PortGroup ports, final Set<SocketLock> locks) {
    InetAddress hostTmp = null;
    try {
      hostTmp = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      Util.handleException(e);
    }
    this.host = hostTmp;
    this.childPort = (int) ports.child;
    this.parentPort = (int) ports.parent;
    this.parentAltPort = (int) ports.parentAlt;
    this.locks = locks;
  }

  private SocketFactory(SocketFactory other) {
    this.host = other.host;
    this.childPort = other.childPort;
    this.parentPort = other.parentPort;
    this.parentAltPort = other.parentAltPort;
    this.locks = other.locks;
  }

  private Object readResolve() {
    return new SocketFactory(this);
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
    if (holdsLock()) {
      return createSocket(clientSocket, parentPort, childPort, false);
    }
    return createSocket(clientAltSocket, parentAltPort, childPort, true);
  }

  private boolean holdsLock() {
    synchronized (locks) {
      for (SocketLock lock : locks) {
        if (Thread.holdsLock(lock)) {
          return true;
        }
      }
    }
    return false;
  }

  private Socket createSocket(AtomicReference<Socket> socket,
      int localPort, int foreignPort, boolean background) throws IOException {
    final int retries = 15;
    for (int i = 1, sleep = 2; i <= retries; i++, sleep *= 2) {
      try {
        if (!background) {
          Util.close(socket.get());
        }
        socket.set(new Socket());
        socket.get().setReuseAddress(true);
        socket.get().setTcpNoDelay(true);
        socket.get().setKeepAlive(true);
        socket.get().bind(new InetSocketAddress(host, localPort));
        socket.get().connect(new InetSocketAddress(host, foreignPort));
        return socket.get();
      } catch (IOException e) {
        try {
          if (background || i == retries) {
            throw e;
          }
          try {
            Thread.sleep(sleep);
          } catch (InterruptedException e2) {}
        } finally {
          Util.close(socket.get());
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
