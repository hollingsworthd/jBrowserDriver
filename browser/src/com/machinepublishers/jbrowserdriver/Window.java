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

import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.machinepublishers.jbrowserdriver.config.JavaFxObject;

public class Window implements org.openqa.selenium.WebDriver.Window {
  private final AtomicReference<JavaFxObject> stage;
  private final long settingsId;

  Window(final AtomicReference<JavaFxObject> stage, final long settingsId) {
    this.stage = stage;
    this.settingsId = settingsId;
  }

  public void close() {
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
