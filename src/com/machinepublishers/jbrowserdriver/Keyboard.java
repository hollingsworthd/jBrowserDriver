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

class Keyboard implements org.openqa.selenium.interactions.Keyboard {

  private final KeyboardRemote remote;
  private final Logs logs;

  Keyboard(KeyboardRemote remote, Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  @Override
  public void pressKey(CharSequence key) {
    try {
      remote.pressKey(key);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void releaseKey(CharSequence key) {
    try {
      remote.releaseKey(key);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void sendKeys(CharSequence... keys) {
    try {
      remote.sendKeys(keys);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  boolean isShiftPressed() {
    try {
      return remote.isShiftPressed();
    } catch (RemoteException e) {
      logs.exception(e);
      return false;
    }
  }
}
