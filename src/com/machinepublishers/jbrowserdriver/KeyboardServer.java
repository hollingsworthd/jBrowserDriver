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
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.Keys;

class KeyboardServer extends RemoteObject implements KeyboardRemote,
    org.openqa.selenium.interactions.Keyboard {

  private final AtomicReference<Robot> robot;
  private boolean shiftPressed;
  private final Object lock = new Object();

  KeyboardServer(final AtomicReference<Robot> robot) throws RemoteException {
    this.robot = robot;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void pressKey(CharSequence key) {
    synchronized (lock) {
      if (!shiftPressed) {
        shiftPressed = Keys.SHIFT.equals(key) || Keys.LEFT_SHIFT.equals(key);
      }
    }
    robot.get().keysPress(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void releaseKey(CharSequence key) {
    synchronized (lock) {
      if (shiftPressed) {
        shiftPressed = !Keys.SHIFT.equals(key) && !Keys.LEFT_SHIFT.equals(key);
      }
    }
    robot.get().keysRelease(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendKeys(CharSequence... keys) {
    robot.get().keysType(keys);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isShiftPressed() {
    synchronized (lock) {
      return shiftPressed;
    }
  }
}
