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

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class TargetLocator implements org.openqa.selenium.WebDriver.TargetLocator {
  private final TargetLocatorRemote remote;
  private final JBrowserDriver driver;
  private final Logs logs;

  TargetLocator(TargetLocatorRemote remote, JBrowserDriver driver, Logs logs) {
    this.remote = remote;
    this.driver = driver;
    this.logs = logs;
  }

  @Override
  public WebElement activeElement() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Alert alert() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WebDriver defaultContent() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WebDriver frame(int arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WebDriver frame(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WebDriver frame(WebElement arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WebDriver parentFrame() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WebDriver window(String windowHandle) {
    try {
      remote.window(windowHandle);
      return driver;
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }
}
