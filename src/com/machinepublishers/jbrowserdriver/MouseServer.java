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

  /**
   * {@inheritDoc}
   */
  @Override
  public void click(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseClick(MouseButton.LEFT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void click(CoordinatesRemote coords) {
    if (coords != null) {
      click(new com.machinepublishers.jbrowserdriver.Coordinates(coords,
              new Logs(LogsServer.instance())));
    } else {
      click((Coordinates)null);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void contextClick(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseClick(MouseButton.RIGHT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void contextClick(CoordinatesRemote coords) {
    contextClick(new com.machinepublishers.jbrowserdriver.Coordinates(coords,
        new Logs(LogsServer.instance())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doubleClick(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseClick(MouseButton.LEFT);
    robot.get().mouseClick(MouseButton.LEFT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doubleClick(CoordinatesRemote coords) {
    doubleClick(new com.machinepublishers.jbrowserdriver.Coordinates(coords,
        new Logs(LogsServer.instance())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseDown(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mousePress(MouseButton.LEFT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseDown(CoordinatesRemote coords) {
    mouseDown(new com.machinepublishers.jbrowserdriver.Coordinates(coords,
        new Logs(LogsServer.instance())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseMove(Coordinates coords) {
    robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseMove(CoordinatesRemote coords) {
    mouseMove(new com.machinepublishers.jbrowserdriver.Coordinates(coords,
        new Logs(LogsServer.instance())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseMove(Coordinates coords, long xOffset, long yOffset) {
    if (coords == null || coords.onPage() == null) {
      robot.get().mouseMoveBy(xOffset, yOffset);
    } else {
      robot.get().mouseMove(coords.onPage().x + xOffset, coords.onPage().y + yOffset);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseMove(CoordinatesRemote coords, long xOffset, long yOffset) {
    mouseMove(new com.machinepublishers.jbrowserdriver.Coordinates(coords,
        new Logs(LogsServer.instance())), xOffset, yOffset);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseUp(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseRelease(MouseButton.LEFT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseUp(CoordinatesRemote coords) {
    mouseUp(new com.machinepublishers.jbrowserdriver.Coordinates(coords,
        new Logs(LogsServer.instance())));
  }

}
