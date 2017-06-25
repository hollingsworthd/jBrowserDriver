/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2017 jBrowserDriver committers
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

import java.rmi.RemoteException;
import java.util.concurrent.Executors;

public class HeartbeatServer extends RemoteObject implements HeartbeatRemote {

  private volatile long lastHeartbeat;

  public HeartbeatServer() throws RemoteException {
    Executors.newSingleThreadExecutor().execute(new Runnable() {
      @Override
      public void run() {
        try {
          // give 60 seconds for initial heartbeat
          Thread.sleep(60000);
        } catch (InterruptedException e) {}
        
        while (true) {
          if (System.currentTimeMillis() - lastHeartbeat > 60000) {
            // no heartbeat received in the last 60 seconds
            System.exit(1);
          }
          
          try {
            // sleep a bit to avoid busy loop
            Thread.sleep(5000);
          } catch (InterruptedException e) {}
        }
      }
    });
  }

  // this is called every ~5 seconds by parent process
  @Override
  public void heartbeat() throws RemoteException {
    lastHeartbeat = System.currentTimeMillis();
  }
}
