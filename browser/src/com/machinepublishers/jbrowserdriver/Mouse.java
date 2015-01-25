/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.interactions.internal.Coordinates;

import com.machinepublishers.jbrowserdriver.Robot.MouseButton;

public class Mouse implements org.openqa.selenium.interactions.Mouse {
  private final AtomicReference<Robot> robot;

  Mouse(final AtomicReference<Robot> robot) {
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
  public void contextClick(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseClick(MouseButton.RIGHT);
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
  public void mouseDown(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mousePress(MouseButton.LEFT);
  }

  @Override
  public void mouseMove(Coordinates coords) {
    robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
  }

  @Override
  public void mouseMove(Coordinates coords, long xOffset, long yOffset) {
    robot.get().mouseMove(coords.onPage().x + xOffset, coords.onPage().y + yOffset);
  }

  @Override
  public void mouseUp(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseRelease(MouseButton.LEFT);
  }

}
