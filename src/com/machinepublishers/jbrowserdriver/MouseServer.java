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

import org.openqa.selenium.interactions.internal.Coordinates;

import com.machinepublishers.jbrowserdriver.Robot.MouseButton;

class MouseServer extends UnicastRemoteObject implements MouseRemote,
    org.openqa.selenium.interactions.Mouse {
  private final AtomicReference<Robot> robot;

  MouseServer(final AtomicReference<Robot> robot) throws RemoteException {
    this.robot = robot;
  }

  @Override
  public void click(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseClick(MouseButton.LEFT);
  }

  @Override
  public void click(CoordinatesRemote coords, LogsRemote logs) {
    click(new com.machinepublishers.jbrowserdriver.Coordinates(coords, logs));
  }

  @Override
  public void contextClick(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseClick(MouseButton.RIGHT);
  }

  @Override
  public void contextClick(CoordinatesRemote coords, LogsRemote logs) {
    contextClick(new com.machinepublishers.jbrowserdriver.Coordinates(coords, logs));
  }

  @Override
  public void doubleClick(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseClick(MouseButton.LEFT);
    robot.get().mouseClick(MouseButton.LEFT);
  }

  @Override
  public void doubleClick(CoordinatesRemote coords, LogsRemote logs) {
    doubleClick(new com.machinepublishers.jbrowserdriver.Coordinates(coords, logs));
  }

  @Override
  public void mouseDown(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mousePress(MouseButton.LEFT);
  }

  @Override
  public void mouseDown(CoordinatesRemote coords, LogsRemote logs) {
    mouseDown(new com.machinepublishers.jbrowserdriver.Coordinates(coords, logs));
  }

  @Override
  public void mouseMove(Coordinates coords) {
    robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
  }

  @Override
  public void mouseMove(CoordinatesRemote coords, LogsRemote logs) {
    mouseMove(new com.machinepublishers.jbrowserdriver.Coordinates(coords, logs));
  }

  @Override
  public void mouseMove(Coordinates coords, long xOffset, long yOffset) {
    if (coords == null || coords.onPage() == null) {
      robot.get().mouseMoveBy(xOffset, yOffset);
    } else {
      robot.get().mouseMove(coords.onPage().x + xOffset, coords.onPage().y + yOffset);
    }
  }

  @Override
  public void mouseMove(CoordinatesRemote coords, long xOffset, long yOffset, LogsRemote logs) {
    mouseMove(new com.machinepublishers.jbrowserdriver.Coordinates(coords, logs), xOffset, yOffset);
  }

  @Override
  public void mouseUp(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseRelease(MouseButton.LEFT);
  }

  @Override
  public void mouseUp(CoordinatesRemote coords, LogsRemote logs) {
    mouseUp(new com.machinepublishers.jbrowserdriver.Coordinates(coords, logs));
  }

}
