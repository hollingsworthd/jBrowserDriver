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

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement activeElement() {
    try {
      return Element.constructElement(remote.activeElement(), driver, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Alert alert() {
    try {
      return new Alert(remote.alert(), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver defaultContent() {
    try {
      remote.defaultContent();
      return driver;
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver frame(int index) {
    try {
      remote.frame(index);
      return driver;
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver frame(String nameOrId) {
    try {
      remote.frame(nameOrId);
      return driver;
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver frame(WebElement element) {
    ((Element) element).activate();
    return driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver parentFrame() {
    try {
      remote.parentFrame();
      return driver;
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
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
