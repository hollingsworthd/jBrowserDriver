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

import javafx.stage.Stage;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import com.machinepublishers.jbrowserdriver.Util.Sync;

public class Window implements org.openqa.selenium.WebDriver.Window {
  private final Stage stage;

  public Window(Stage stage) {
    this.stage = stage;
  }

  public void close() {
    Util.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        stage.close();
        return null;
      }
    });
  }

  @Override
  public Point getPosition() {
    return Util.exec(new Sync<Point>() {
      @Override
      public Point perform() {
        return new Point((int) Math.rint(stage.getX()), (int) Math.rint(stage.getY()));
      }
    });
  }

  @Override
  public Dimension getSize() {
    return Util.exec(new Sync<Dimension>() {
      @Override
      public Dimension perform() {
        return new Dimension((int) Math.rint(stage.getWidth()), (int) Math.rint(stage.getHeight()));
      }
    });
  }

  @Override
  public void maximize() {
    Util.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        stage.setMaximized(true);
        return null;
      }
    });
  }

  @Override
  public void setPosition(final Point point) {
    Util.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        stage.setMaximized(false);
        stage.setX(point.getX());
        stage.setY(point.getY());
        return null;
      }
    });
  }

  @Override
  public void setSize(final Dimension dimension) {
    Util.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        stage.setMaximized(false);
        stage.setWidth(dimension.getWidth());
        stage.setHeight(dimension.getHeight());
        return null;
      }
    });
  }

}
