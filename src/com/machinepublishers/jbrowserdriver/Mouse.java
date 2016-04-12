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

import org.openqa.selenium.interactions.internal.Coordinates;

class Mouse implements org.openqa.selenium.interactions.Mouse {
  private final MouseRemote remote;

  Mouse(MouseRemote remote) {
    this.remote = remote;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void click(Coordinates coords) {
    try {
      CoordinatesRemote coordsRemote = coords == null
          ? null : ((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote;
      remote.click(coordsRemote);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void contextClick(Coordinates coords) {
    try {
      CoordinatesRemote coordsRemote = coords == null
          ? null : ((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote;
      remote.contextClick(coordsRemote);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doubleClick(Coordinates coords) {
    try {
      CoordinatesRemote coordsRemote = coords == null
          ? null : ((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote;
      remote.doubleClick(coordsRemote);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseDown(Coordinates coords) {
    try {
      CoordinatesRemote coordsRemote = coords == null
          ? null : ((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote;
      remote.mouseDown(coordsRemote);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseMove(Coordinates coords) {
    try {
      CoordinatesRemote coordsRemote = coords == null
          ? null : ((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote;
      remote.mouseMove(coordsRemote);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseMove(Coordinates coords, long xOffset, long yOffset) {
    try {
      CoordinatesRemote coordsRemote = coords == null
          ? null : ((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote;
      remote.mouseMove(coordsRemote, xOffset, yOffset);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void mouseUp(Coordinates coords) {
    try {
      CoordinatesRemote coordsRemote = coords == null
          ? null : ((com.machinepublishers.jbrowserdriver.Coordinates) coords).remote;
      remote.mouseUp(coordsRemote);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}
