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

import com.machinepublishers.jbrowserdriver.Robot.MouseButton;

class MouseServer extends RemoteObject implements MouseRemote,
    org.openqa.selenium.interactions.Mouse {
  private final AtomicReference<Robot> robot;

  MouseServer(final AtomicReference<Robot> robot) throws RemoteException {
    this.robot = robot;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void click(org.openqa.selenium.interactions.internal.Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.inViewPort());
    }
    robot.get().mouseClick(MouseButton.LEFT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remoteClick(Coordinates coords) {
    click(coords);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void contextClick(org.openqa.selenium.interactions.internal.Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.inViewPort());
    }
    robot.get().mouseClick(MouseButton.RIGHT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remoteContextClick(Coordinates coords) {
    contextClick(coords);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doubleClick(org.openqa.selenium.interactions.internal.Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.inViewPort());
    }
    robot.get().mouseClick(MouseButton.LEFT);
    robot.get().mouseClick(MouseButton.LEFT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remoteDoubleClick(Coordinates coords) {
    doubleClick(coords);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseDown(org.openqa.selenium.interactions.internal.Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.inViewPort());
    }
    robot.get().mousePress(MouseButton.LEFT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remoteMouseDown(Coordinates coords) {
    mouseDown(coords);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseMove(org.openqa.selenium.interactions.internal.Coordinates coords) {
    robot.get().mouseMove(coords.inViewPort());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remoteMouseMove(Coordinates coords) {
    mouseMove(coords);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseMove(org.openqa.selenium.interactions.internal.Coordinates coords, long xOffset, long yOffset) {
    if (coords != null) {
      robot.get().mouseMove(coords.inViewPort());
    } else {
      robot.get().mouseMoveBy(xOffset, yOffset);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remoteMouseMove(Coordinates coords, long xOffset, long yOffset) {
    mouseMove(coords, xOffset, yOffset);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseUp(org.openqa.selenium.interactions.internal.Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.inViewPort());
    }
    robot.get().mouseRelease(MouseButton.LEFT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remoteMouseUp(Coordinates coords) {
    mouseUp(coords);
  }
}
