/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
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

import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.sun.javafx.webkit.Accessor;

public class DynamicApplication extends Application {
  private static final Object lock = new Object();
  private static Stage myStage;
  private static WebView myView;
  private int width;
  private int height;
  private boolean headless;
  private long settingsId;

  public static Stage getStage() {
    synchronized (lock) {
      while (myStage == null) {
        try {
          lock.wait();
        } catch (InterruptedException e) {}
      }
      return myStage;
    }
  }

  public static WebView getView() {
    synchronized (lock) {
      while (myView == null) {
        try {
          lock.wait();
        } catch (InterruptedException e) {}
      }
      return myView;
    }
  }

  @Override
  public void init() throws Exception {
    List<String> params = getParameters().getRaw();
    width = Integer.parseInt(params.get(0));
    height = Integer.parseInt(params.get(1));
    headless = Boolean.parseBoolean(params.get(2));
    settingsId = Long.parseLong(params.get(3));
  }

  @Override
  public void start(Stage sstage) throws Exception {
    Stage stage = new Stage();
    stage.setOpacity(0d);
    WebView view = new WebView();
    view.setCache(false);
    view.setOpacity(0d);
    if (headless) {
      stage.initStyle(StageStyle.UNDECORATED);
    }
    StackPane root = new StackPane();
    root.setCache(false);
    root.setOpacity(0d);
    WebEngine engine = view.getEngine();
    engine.getHistory().setMaxSize(0);
    engine.setUserAgent(Long.toString(settingsId));
    Accessor.getPageFor(engine).setDeveloperExtrasEnabled(false);
    Accessor.getPageFor(engine).setUsePageCache(false);
    root.getChildren().add(view);
    stage.setScene(new Scene(root, width, height));
    stage.sizeToScene();
    engine.titleProperty().addListener(new DynamicTitleListener(stage));
    stage.show();

    synchronized (lock) {
      myStage = stage;
      myView = view;
      lock.notifyAll();
    }
  }
}
