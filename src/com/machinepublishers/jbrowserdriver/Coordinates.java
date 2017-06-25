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

import java.io.Serializable;

class Coordinates implements org.openqa.selenium.interactions.internal.Coordinates, Serializable {

  private final Point inViewport;
  private final ElementRemote remote;
  private final SocketLock lock;

  Coordinates(ElementRemote remote, SocketLock lock) {
    this.remote = remote;
    this.lock = lock;
    this.inViewport = null;
  }

  Coordinates(org.openqa.selenium.interactions.internal.Coordinates coords) {
    this.inViewport = coords.inViewPort() == null ? null : new Point(coords.inViewPort());
    this.remote = null;
    this.lock = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point onScreen() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point inViewPort() {
    if (inViewport == null) {
      synchronized (lock.validated()) {
        try {
          return remote.locate().toSelenium();
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    } else if (inViewport != null) {
      return inViewport.toSelenium();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point onPage() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getAuxiliary() {
    return null;
  }
}
