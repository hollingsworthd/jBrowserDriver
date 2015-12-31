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

import org.openqa.selenium.interactions.internal.Coordinates;

class Mouse implements org.openqa.selenium.interactions.Mouse {
  private final MouseRemote remote;
  private final Logs logs;

  Mouse(MouseRemote remote, Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  @Override
  public void click(Coordinates coords) {
    try {
      remote.click(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void contextClick(Coordinates coords) {
    try {
      remote.contextClick(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void doubleClick(Coordinates coords) {
    try {
      remote.doubleClick(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void mouseDown(Coordinates coords) {
    try {
      remote.mouseDown(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void mouseMove(Coordinates coords) {
    try {
      remote.mouseMove(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void mouseMove(Coordinates coords, long xOffset, long yOffset) {
    try {
      remote.mouseMove(
          ((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote, xOffset, yOffset);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void mouseUp(Coordinates coords) {
    try {
      remote.mouseUp(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }
}
