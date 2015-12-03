/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.Keys;

class KeyboardServer extends UnicastRemoteObject implements KeyboardRemote,
    org.openqa.selenium.interactions.Keyboard {

  private final AtomicReference<Robot> robot;
  private boolean shiftPressed;
  private final Object lock = new Object();

  KeyboardServer(final AtomicReference<Robot> robot) throws RemoteException {
    this.robot = robot;
  }

  @Override
  public void pressKey(CharSequence key) {
    synchronized (lock) {
      if (!shiftPressed) {
        shiftPressed = Keys.SHIFT.equals(key) || Keys.LEFT_SHIFT.equals(key);
      }
    }
    robot.get().keysPress(key);
  }

  @Override
  public void releaseKey(CharSequence key) {
    synchronized (lock) {
      if (shiftPressed) {
        shiftPressed = !Keys.SHIFT.equals(key) && !Keys.LEFT_SHIFT.equals(key);
      }
    }
    robot.get().keysRelease(key);
  }

  @Override
  public void sendKeys(CharSequence... keys) {
    robot.get().keysType(keys);
  }

  @Override
  public boolean isShiftPressed() {
    synchronized (lock) {
      return shiftPressed;
    }
  }
}
