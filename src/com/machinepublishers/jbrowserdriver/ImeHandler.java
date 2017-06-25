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

import java.util.ArrayList;
import java.util.List;

class ImeHandler implements org.openqa.selenium.WebDriver.ImeHandler {
  private final ImeHandlerRemote remote;
  private final SocketLock lock;

  ImeHandler(ImeHandlerRemote remote, SocketLock lock) {
    this.remote = remote;
    this.lock = lock;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void activateEngine(String name) {
    try {
      synchronized (lock.validated()) {
        remote.activateEngine(name);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deactivate() {
    try {
      synchronized (lock.validated()) {
        remote.deactivate();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getActiveEngine() {
    try {
      synchronized (lock.validated()) {
        return remote.getActiveEngine();
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
  public List<String> getAvailableEngines() {
    try {
      synchronized (lock.validated()) {
        return remote.getAvailableEngines();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<String>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isActivated() {
    try {
      synchronized (lock.validated()) {
        return remote.isActivated();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return false;
    }
  }
}
