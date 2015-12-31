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
import java.util.List;

class ImeHandler implements org.openqa.selenium.WebDriver.ImeHandler {
  private final ImeHandlerRemote remote;
  private final Logs logs;

  ImeHandler(ImeHandlerRemote remote, Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  @Override
  public void activateEngine(String name) {
    try {
      remote.activateEngine(name);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void deactivate() {
    try {
      remote.deactivate();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public String getActiveEngine() {
    try {
      return remote.getActiveEngine();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<String> getAvailableEngines() {
    try {
      return remote.getAvailableEngines();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public boolean isActivated() {
    try {
      return remote.isActivated();
    } catch (RemoteException e) {
      logs.exception(e);
      return false;
    }
  }
}
