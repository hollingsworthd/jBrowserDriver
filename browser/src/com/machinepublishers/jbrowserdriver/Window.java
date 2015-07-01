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

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

class Window implements org.openqa.selenium.WebDriver.Window {
  private final AtomicReference<JavaFxObject> stage;
  private final long settingsId;

  Window(final AtomicReference<JavaFxObject> stage, final long settingsId) {
    this.stage = stage;
    this.settingsId = settingsId;
  }

  void close() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().call("close");
        return null;
      }
    }, settingsId);
  }

  @Override
  public Point getPosition() {
    return Util.exec(Pause.NONE, new Sync<Point>() {
      @Override
      public Point perform() {
        return new Point((int) Math.rint((Double) stage.get().call("getX").unwrap()),
            (int) Math.rint((Double) stage.get().call("getY").unwrap()));
      }
    }, settingsId);
  }

  @Override
  public Dimension getSize() {
    return Util.exec(Pause.NONE, new Sync<Dimension>() {
      @Override
      public Dimension perform() {
        return new Dimension((int) Math.rint((Double) stage.get().call("getWidth").unwrap()),
            (int) Math.rint((Double) stage.get().call("getHeight").unwrap()));
      }
    }, settingsId);
  }

  @Override
  public void maximize() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().call("setMaximized", true);
        return null;
      }
    }, settingsId);
  }

  @Override
  public void setPosition(final Point point) {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().call("setMaximized", false);
        stage.get().call("setX", point.getX());
        stage.get().call("setY", point.getY());
        return null;
      }
    }, settingsId);
  }

  @Override
  public void setSize(final Dimension dimension) {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().call("setMaximized", false);
        stage.get().call("setWidth", dimension.getWidth());
        stage.get().call("setHeight", dimension.getHeight());
        return null;
      }
    }, settingsId);
  }

}
