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
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class TimeoutsServer extends UnicastRemoteObject implements TimeoutsRemote,
    org.openqa.selenium.WebDriver.Timeouts {
  private AtomicLong implicit = new AtomicLong();
  private AtomicLong load = new AtomicLong();
  private AtomicLong script = new AtomicLong();

  TimeoutsServer() throws RemoteException {}

  @Override
  public TimeoutsServer implicitlyWait(long duration, TimeUnit unit) {
    implicit.set(unit.convert(duration, TimeUnit.MILLISECONDS));
    return this;
  }

  @Override
  public TimeoutsServer pageLoadTimeout(long duration, TimeUnit unit) {
    load.set(unit.convert(duration, TimeUnit.MILLISECONDS));
    return this;
  }

  @Override
  public TimeoutsServer setScriptTimeout(long duration, TimeUnit unit) {
    script.set(unit.convert(duration, TimeUnit.MILLISECONDS));
    return this;
  }

  @Override
  public long getImplicitlyWaitMS() {
    return implicit.get();
  }

  @Override
  public long getPageLoadTimeoutMS() {
    return load.get();
  }

  @Override
  public long getScriptTimeoutMS() {
    return script.get();
  }

  @Override
  public AtomicLong getImplicitlyWaitObjMS() {
    return implicit;
  }

  @Override
  public AtomicLong getPageLoadTimeoutObjMS() {
    return load;
  }

  @Override
  public AtomicLong getScriptTimeoutObjMS() {
    return script;
  }
}
