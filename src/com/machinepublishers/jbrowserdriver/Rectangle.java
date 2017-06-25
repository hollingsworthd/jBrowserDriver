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

class Rectangle implements Serializable {

  private final int x;
  private final int y;
  private final int height;
  private final int width;

  Rectangle(int x, int y, int height, int width) {
    this.x = x;
    this.y = y;
    this.height = height;
    this.width = width;
  }

  Rectangle(org.openqa.selenium.Rectangle rect) {
    this.x = rect.x;
    this.y = rect.y;
    this.height = rect.height;
    this.width = rect.width;
  }

  org.openqa.selenium.Rectangle toSelenium() {
    return new org.openqa.selenium.Rectangle(x, y, height, width);
  }
}
