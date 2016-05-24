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

import java.io.Serializable;

class Coordinates implements org.openqa.selenium.interactions.internal.Coordinates, Serializable {

  private final Point page;

  Coordinates(int pageX, int pageY) {
    this.page = new Point(pageX, pageY);
  }

  Coordinates(org.openqa.selenium.interactions.internal.Coordinates coords) {
    this.page = coords.onPage() == null ? null : new Point(coords.onPage());
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
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point onPage() {
    return page.toSelenium();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getAuxiliary() {
    return null;
  }
}
