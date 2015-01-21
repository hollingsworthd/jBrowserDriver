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

public class Keyboard implements org.openqa.selenium.interactions.Keyboard {

  private final Robot robot;

  Keyboard(Robot robot) {
    this.robot = robot;
  }

  @Override
  public void pressKey(CharSequence key) {
    robot.keysPress(key);
  }

  @Override
  public void releaseKey(CharSequence key) {
    robot.keysRelease(key);
  }

  @Override
  public void sendKeys(CharSequence... keys) {
    robot.keysType(keys);
  }

}
