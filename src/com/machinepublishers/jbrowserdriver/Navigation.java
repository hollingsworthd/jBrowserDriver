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

import java.net.URL;
import java.rmi.RemoteException;

class Navigation implements org.openqa.selenium.WebDriver.Navigation {
  private final NavigationRemote remote;
  private final Logs logs;

  Navigation(NavigationRemote remote, Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void back() {
    try {
      remote.back();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void forward() {
    try {
      remote.forward();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void refresh() {
    try {
      remote.refresh();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void to(String url) {
    try {
      remote.to(url);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void to(URL url) {
    try {
      remote.to(url);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }
}
