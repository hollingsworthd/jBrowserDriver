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

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;

import com.machinepublishers.glass.ui.monocle.NativePlatform;
import com.machinepublishers.glass.ui.monocle.NativePlatformFactory;
import com.sun.glass.ui.Screen;
import com.sun.javafx.webkit.Accessor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Internal use only.
 * 
 * @deprecated
 */
public class App extends Application {
  private static final int HISTORY_SIZE = 8;
  private static final Object lock = new Object();
  private static Stage myStage;
  private static WebView myView;
  private int width;
  private int height;
  private boolean headless;

  /**
   * Internal use only.
   */
  public App() {

  }

  static Stage getStage() {
    synchronized (lock) {
      while (myStage == null) {
        try {
          lock.wait();
        } catch (InterruptedException e) {}
      }
      return myStage;
    }
  }

  static WebView getView() {
    synchronized (lock) {
      while (myView == null) {
        try {
          lock.wait();
        } catch (InterruptedException e) {}
      }
      return myView;
    }
  }

  void init(int width, int height, boolean headless) {
    this.width = width;
    this.height = height;
    this.headless = headless;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws Exception {
    List<String> params = getParameters().getRaw();
    width = Integer.parseInt(params.get(0));
    height = Integer.parseInt(params.get(1));
    headless = Boolean.parseBoolean(params.get(2));
  }

  void start() throws Exception {
    start(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start(Stage stage) throws Exception {
    if (headless) {
      System.setProperty("headless.geometry", width + "x" + height);
      NativePlatform platform = NativePlatformFactory.getNativePlatform();
      Field field = NativePlatform.class.getDeclaredField("screen");
      field.setAccessible(true);
      field.set(platform, null);
      Method method = Screen.class.getDeclaredMethod("notifySettingsChanged");
      method.setAccessible(true); //before Java 8u20 this method was private
      method.invoke(null);
    }
    if (stage == null) {
      stage = new Stage();
    }
    Platform.setImplicitExit(false);
    WebView view = new WebView();
    view.setCache(false);
    StackPane root = new StackPane();
    root.setCache(false);
    if (headless) {
      stage.initStyle(StageStyle.UNDECORATED);
    }
    WebEngine engine = view.getEngine();
    File style = File.createTempFile("jbd_style_", ".css");
    style.deleteOnExit();
    Files.write(style.toPath(),
        "body::-webkit-scrollbar {width: 0px !important;height:0px !important;}".getBytes("utf-8"));
    engine.setUserStyleSheetLocation(style.toPath().toUri().toURL().toExternalForm());
    engine.getHistory().setMaxSize(HISTORY_SIZE);
    Accessor.getPageFor(engine).setDeveloperExtrasEnabled(false);
    Accessor.getPageFor(engine).setUsePageCache(false);
    root.getChildren().add(view);
    stage.setScene(new Scene(root, width, height));
    stage.sizeToScene();
    engine.titleProperty().addListener(new TitleListener(stage));
    stage.show();
    synchronized (lock) {
      myStage = stage;
      myView = view;
      lock.notifyAll();
    }
  }
}
