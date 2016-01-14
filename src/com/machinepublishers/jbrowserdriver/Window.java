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

class Window implements org.openqa.selenium.WebDriver.Window {
  private final WindowRemote remote;
  private final Logs logs;

  Window(WindowRemote remote, Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point getPosition() {
    try {
      return remote.getPosition();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Dimension getSize() {
    try {
      return remote.getSize();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void maximize() {
    try {
      remote.maximize();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPosition(final org.openqa.selenium.Point point) {
    try {
      remote.setPosition(new Point(point));
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSize(final org.openqa.selenium.Dimension dimension) {
    try {
      remote.setSize(new Dimension(dimension));
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

}
