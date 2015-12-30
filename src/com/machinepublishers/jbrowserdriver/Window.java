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

class Window implements org.openqa.selenium.WebDriver.Window {
  private final WindowRemote remote;
  private final Logs logs;

  Window(WindowRemote remote, Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  @Override
  public Point getPosition() {
    try {
      return remote.getPosition();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Dimension getSize() {
    try {
      return remote.getSize();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public void maximize() {
    try {
      remote.maximize();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void setPosition(final org.openqa.selenium.Point point) {
    try {
      remote.setPosition(new Point(point));
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void setSize(final org.openqa.selenium.Dimension dimension) {
    try {
      remote.setSize(new Dimension(dimension));
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

}
