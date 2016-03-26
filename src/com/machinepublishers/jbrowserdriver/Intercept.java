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

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class Intercept extends UnicastRemoteObject implements InterceptRemote {

  private final Object lock = new Object();
  private final List<RequestInterceptor> requestInterceptors = new ArrayList<RequestInterceptor>();
  private final List<ResponseInterceptor> responseInterceptors = new ArrayList<ResponseInterceptor>();
  private final AtomicInteger objectCount = new AtomicInteger();
  private final AtomicBoolean initialState = new AtomicBoolean(true);
  private final Registry registry;

  protected Intercept() throws RemoteException {
    super(9999);
    Registry registryTmp = null;
    try {
      registryTmp = LocateRegistry.createRegistry(9999);
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    }
    registry = registryTmp;
    registry.rebind("Intercept", this);
  }

  void allocate() {
    if (objectCount.getAndIncrement() == 0
        && initialState.compareAndSet(false, false)) {
      try {
        exportObject(this, 9999);
        registry.rebind("Intercept", this);
      } catch (Throwable t) {
        Logs.fatal(t);
      }
    }
  }

  void deallocate() {
    if (objectCount.getAndDecrement() == 1) {
      try {
        registry.unbind("Intercept");
        unexportObject(this, true);
      } catch (Throwable t) {
        Logs.fatal(t);
      }
    }
  }

  void addRequestInterceptor(RequestInterceptor responseInterceptor) {
    synchronized (lock) {
      requestInterceptors.add(responseInterceptor);
    }
  }

  void addResponseInterceptor(ResponseInterceptor responseInterceptor) {
    synchronized (lock) {
      responseInterceptors.add(responseInterceptor);
    }
  }

  void removeAll() {
    synchronized (lock) {
      requestInterceptors.clear();
      responseInterceptors.clear();
    }
  }

  @Override
  public void intercept() {
    //TODO
  }
}
