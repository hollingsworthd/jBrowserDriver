/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * jBrowserDriver is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking jBrowserDriver statically or dynamically with other modules is making a combined work
 *   based on jBrowserDriver. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of jBrowserDriver with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on jBrowserDriver. If you modify jBrowserDriver, you may not
 *   extend this exception to your modified version of jBrowserDriver.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.interactions.internal.Coordinates;

import com.machinepublishers.jbrowserdriver.Robot.MouseButton;

class Mouse implements org.openqa.selenium.interactions.Mouse {
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
    if (coords == null || coords.onPage() == null) {
      robot.get().mouseMoveBy(xOffset, yOffset);
    } else {
      robot.get().mouseMove(coords.onPage().x + xOffset, coords.onPage().y + yOffset);
    }
  }

  @Override
  public void mouseUp(Coordinates coords) {
    if (coords != null) {
      robot.get().mouseMove(coords.onPage().x, coords.onPage().y);
    }
    robot.get().mouseRelease(MouseButton.LEFT);
  }

}
