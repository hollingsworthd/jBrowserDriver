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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point getPosition() {
    return Util.exec(Pause.NONE, new AtomicInteger(-1), new Sync<org.openqa.selenium.Point>() {
      @Override
      public org.openqa.selenium.Point perform() {
        return new org.openqa.selenium.Point((int) Math.rint((Double) stage.get().getX()),
            (int) Math.rint((Double) stage.get().getY()));
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point remoteGetPosition() {
    return new Point(getPosition());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Dimension getSize() {
    return Util.exec(Pause.NONE, new AtomicInteger(-1), new Sync<org.openqa.selenium.Dimension>() {
      @Override
      public org.openqa.selenium.Dimension perform() {
        return new org.openqa.selenium.Dimension((int) Math.rint((Double) stage.get().getWidth()),
            (int) Math.rint((Double) stage.get().getHeight()));
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Dimension remoteGetSize() {
    return new Dimension(getSize());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void maximize() {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().setMaximized(!stage.get().isMaximized());
        return null;
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPosition(final Point point) {
    setPosition(point.toSelenium());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setPosition(final org.openqa.selenium.Point point) {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        if (!stage.get().isFullScreen()) {
          int screenWidth = SettingsManager.settings().screenWidth();
          int screenHeight = SettingsManager.settings().screenHeight();

          int width = (int) Math.rint((Double) stage.get().getWidth());
          int height = (int) Math.rint((Double) stage.get().getHeight());

          int newX = Math.max(0, Math.min(screenWidth - width, point.getX()));
          int newY = Math.max(0, Math.min(screenHeight - height, point.getY()));

          stage.get().hide();
          stage.get().setMaximized(false);
          stage.get().setX(newX);
          stage.get().setY(newY);
          stage.get().show();
        }
        return null;
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSize(final Dimension dimension) {
    setSize(dimension.toSelenium());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSize(final org.openqa.selenium.Dimension dimension) {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        if (!stage.get().isFullScreen()) {
          int screenWidth = SettingsManager.settings().screenWidth();
          int screenHeight = SettingsManager.settings().screenHeight();

          int xPos = (int) Math.rint((Double) stage.get().getX());
          int yPos = (int) Math.rint((Double) stage.get().getY());

          if ((dimension.getWidth() > screenWidth - xPos && xPos > 0)
              || (dimension.getHeight() > screenHeight - yPos && yPos > 0)) {
            xPos = 0;
            yPos = 0;
          }

          int newWidth = Math.max(0, Math.min(screenWidth - xPos, dimension.getWidth()));
          int newHeight = Math.max(0, Math.min(screenHeight - yPos, dimension.getHeight()));

          stage.get().hide();
          stage.get().setMaximized(false);
          stage.get().setX(xPos);
          stage.get().setY(yPos);
          stage.get().setWidth(newWidth);
          stage.get().setHeight(newHeight);
          stage.get().show();
        }
        return null;
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fullscreen() {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().setFullScreen(!stage.get().isFullScreen());
        return null;
      }
    });
  }
}
