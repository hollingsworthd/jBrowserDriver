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

import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.AppThread.Sync;

import javafx.stage.Stage;

class WindowServer extends RemoteObject implements WindowRemote,
    org.openqa.selenium.WebDriver.Window {
  private final AtomicReference<Stage> stage;
  private final StatusCode statusCode;

  WindowServer(final AtomicReference<Stage> stage, final StatusCode statusCode)
      throws RemoteException {
    this.stage = stage;
    this.statusCode = statusCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    AppThread.exec(statusCode, new Sync<Object>() {
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
    return remoteGetPosition().toSelenium();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point remoteGetPosition() {
    return AppThread.exec(new Sync<Point>() {
      @Override
      public Point perform() {
        return new Point((int) Math.rint((Double) stage.get().getX()),
            (int) Math.rint((Double) stage.get().getY()));
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Dimension getSize() {
    return remoteGetSize().toSelenium();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Dimension remoteGetSize() {
    return AppThread.exec(new Sync<Dimension>() {
      @Override
      public Dimension perform() {
        return new Dimension((int) Math.rint((Double) stage.get().getWidth()),
            (int) Math.rint((Double) stage.get().getHeight()));
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void maximize() {
    AppThread.exec(new Sync<Object>() {
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
  public void setPosition(org.openqa.selenium.Point point) {
    AppThread.exec(new Sync<Object>() {
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
  public void remoteSetPosition(final Point point) {
    setPosition(point.toSelenium());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setSize(org.openqa.selenium.Dimension dimension) {
    AppThread.exec(new Sync<Object>() {
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
  public void remoteSetSize(final Dimension dimension) {
    setSize(dimension.toSelenium());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fullscreen() {
    AppThread.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        stage.get().setFullScreen(!stage.get().isFullScreen());
        return null;
      }
    });
  }
}
