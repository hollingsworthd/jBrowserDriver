/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

import javafx.stage.Stage;

class WindowServer extends UnicastRemoteObject implements WindowRemote,
    org.openqa.selenium.WebDriver.Window {
  private final AtomicReference<Stage> stage;
  private final AtomicInteger statusCode;

  WindowServer(final AtomicReference<Stage> stage,
      final AtomicInteger statusCode)
          throws RemoteException {
    this.stage = stage;
    this.statusCode = statusCode;
  }

  @Override
  public void close() {
    Util.exec(Pause.SHORT, statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().close();
        return null;
      }
    });
  }

  @Override
  public Point getPosition() {
    return Util.exec(Pause.NONE, new AtomicInteger(-1), new Sync<Point>() {
      @Override
      public Point perform() {
        return new Point((int) Math.rint((Double) stage.get().getX()),
            (int) Math.rint((Double) stage.get().getY()));
      }
    });
  }

  @Override
  public Dimension getSize() {
    return Util.exec(Pause.NONE, new AtomicInteger(-1), new Sync<Dimension>() {
      @Override
      public Dimension perform() {
        return new Dimension((int) Math.rint((Double) stage.get().getWidth()),
            (int) Math.rint((Double) stage.get().getHeight()));
      }
    });
  }

  @Override
  public void maximize() {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().setMaximized(true);
        return null;
      }
    });
  }

  @Override
  public void setPosition(final Point point) {
    setPosition(point);
  }

  @Override
  public void setPosition(final org.openqa.selenium.Point point) {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().setMaximized(false);
        stage.get().setX(point.getX());
        stage.get().setY(point.getY());
        return null;
      }
    });
  }

  @Override
  public void setSize(final Dimension dimension) {
    setSize(dimension);
  }

  @Override
  public void setSize(final org.openqa.selenium.Dimension dimension) {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().setMaximized(false);
        stage.get().setWidth(dimension.getWidth());
        stage.get().setHeight(dimension.getHeight());
        return null;
      }
    });
  }
}
