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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class TargetLocator implements org.openqa.selenium.WebDriver.TargetLocator {
  private final TargetLocatorRemote remote;
  private final JBrowserDriver driver;
  private final SocketLock lock;

  TargetLocator(TargetLocatorRemote remote, JBrowserDriver driver, SocketLock lock) {
    this.remote = remote;
    this.driver = driver;
    this.lock = lock;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement activeElement() {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.activeElement(), driver, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Alert alert() {
    try {
      synchronized (lock.validated()) {
        return new Alert(remote.alert(), lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver defaultContent() {
    try {
      synchronized (lock.validated()) {
        remote.defaultContent();
      }
      return driver;
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver frame(int index) {
    try {
      synchronized (lock.validated()) {
        remote.frame(index);
      }
      return driver;
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver frame(String nameOrId) {
    try {
      synchronized (lock.validated()) {
        remote.frame(nameOrId);
      }
      return driver;
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver frame(WebElement element) {
    ((Element) Util.unwrap(element)).activate();
    return driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver parentFrame() {
    try {
      synchronized (lock.validated()) {
        remote.parentFrame();
      }
      return driver;
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver window(String windowHandle) {
    try {
      synchronized (lock.validated()) {
        remote.window(windowHandle);
      }
      return driver;
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }
}
