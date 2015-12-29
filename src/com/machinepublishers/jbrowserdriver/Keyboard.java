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

class Keyboard implements org.openqa.selenium.interactions.Keyboard {

  private final KeyboardRemote remote;
  private final LogsServer logs;

  Keyboard(KeyboardRemote remote, LogsServer logs) {
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
