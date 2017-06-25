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

import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class TimeoutsServer extends RemoteObject implements TimeoutsRemote,
    org.openqa.selenium.WebDriver.Timeouts {
  private final AtomicLong implicit = new AtomicLong();
  private final AtomicLong load = new AtomicLong();
  private final AtomicLong script = new AtomicLong();
  private final AtomicLong alert = new AtomicLong();

  TimeoutsServer() throws RemoteException {}

  /**
   * {@inheritDoc}
   */
  @Override
  public TimeoutsServer implicitlyWait(long duration, TimeUnit unit) {
    implicit.set(TimeUnit.MILLISECONDS.convert(duration, unit));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimeoutsServer pageLoadTimeout(long duration, TimeUnit unit) {
    load.set(TimeUnit.MILLISECONDS.convert(duration, unit));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimeoutsServer setScriptTimeout(long duration, TimeUnit unit) {
    script.set(TimeUnit.MILLISECONDS.convert(duration, unit));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimeoutsServer setAlertTimeout(long duration, TimeUnit unit) {
    alert.set(TimeUnit.MILLISECONDS.convert(duration, unit));
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getImplicitlyWaitMS() {
    return implicit.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getPageLoadTimeoutMS() {
    return load.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getScriptTimeoutMS() {
    return script.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getAlertTimeoutMS() {
    return alert.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AtomicLong getImplicitlyWaitObjMS() {
    return implicit;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AtomicLong getPageLoadTimeoutObjMS() {
    return load;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AtomicLong getScriptTimeoutObjMS() {
    return script;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AtomicLong getAlertTimeoutObjMS() {
    return alert;
  }
}
