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

import org.openqa.selenium.interactions.internal.Coordinates;

class Mouse implements org.openqa.selenium.interactions.Mouse {
  private final MouseRemote remote;

  Mouse(MouseRemote remote) {
    this.remote = remote;
  }

  @Override
  public void click(Coordinates coords) {
    try {
      remote.click(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void contextClick(Coordinates coords) {
    try {
      remote.contextClick(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void doubleClick(Coordinates coords) {
    try {
      remote.doubleClick(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void mouseDown(Coordinates coords) {
    try {
      remote.mouseDown(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void mouseMove(Coordinates coords) {
    try {
      remote.mouseMove(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void mouseMove(Coordinates coords, long xOffset, long yOffset) {
    try {
      remote.mouseMove(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote, xOffset, yOffset);
    } catch (RemoteException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void mouseUp(Coordinates coords) {
    try {
      remote.mouseUp(((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }
}
