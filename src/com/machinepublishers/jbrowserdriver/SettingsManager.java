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

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.AppThread.Sync;

import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

class SettingsManager {
  private static final AtomicReference<Settings> settings = new AtomicReference<Settings>();
  private static final AtomicBoolean platformInitialized = new AtomicBoolean();
  private static final AtomicBoolean monocle = new AtomicBoolean();

  static boolean isMonocle() {
    if (!platformInitialized.get()) {
      throw new IllegalStateException();
    }
    return monocle.get();
  }

  static Settings settings() {
    return settings.get();
  }

  static void register(final Settings settings) {
    SettingsManager.settings.set(settings);
    if (settings != null) {
      LogsServer.updateSettings();
      StreamConnection.updateSettings();

      if (settings.headless() && platformInitialized.compareAndSet(false, true)) {
        monocle.set(true);
        System.setProperty("quantum.multithreaded", "false");
        System.setProperty("prism.vsync", "true");
        System.setProperty("javafx.animation.framerate", "1");
        System.setProperty("com.sun.scenario.animation.adaptivepulse", "true");
        System.setProperty("quantum.singlethreaded", "true");
        System.setProperty("prism.threadcheck", "false");
        System.setProperty("prism.dirtyopts", "false");
        System.setProperty("prism.cacheshapes", "false");
        System.setProperty("prism.primtextures", "false");
        System.setProperty("prism.shutdownHook", "false");
        System.setProperty("prism.disableRegionCaching", "true");

        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.allowhidpi", "false");
        System.setProperty("prism.text", "t2k");
        try {
          Class<?> platformFactory = Class.forName("com.sun.glass.ui.PlatformFactory");
          Field field = platformFactory.getDeclaredField("instance");
          field.setAccessible(true);
          field.set(platformFactory, new com.machinepublishers.glass.ui.monocle.MonoclePlatformFactory());
          com.machinepublishers.glass.ui.monocle.NativePlatformFactory.setPlatform(
              new com.machinepublishers.glass.ui.monocle.HeadlessPlatform());
        } catch (Throwable t) {
          Util.handleException(t);
        }
      } else if (platformInitialized.compareAndSet(false, true)) {
        new JFXPanel();
      }
    }
  }

  @SuppressWarnings("deprecation") //App class is for internal use only; it's not actually deprecated
  static void register(
      final AtomicReference<Stage> stage,
      final AtomicReference<WebView> view) {
    ProxyAuth.add(settings.get().proxy());
    if (isMonocle() &&
        com.sun.glass.ui.Application.GetApplication() == null) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            Application.launch(App.class,
                new String[] {
                    Integer.toString(settings.get().screenWidth()),
                    Integer.toString(settings.get().screenHeight()),
                    Boolean.toString(isMonocle()) });
          } catch (Throwable t) {
            LogsServer.instance().exception(t);
          }
        }
      }).start();
    } else {
      final App app = new App();
      app.init(
          settings.get().screenWidth(), settings.get().screenHeight(),
          isMonocle());
      AppThread.exec(new Sync<Object>() {
        public Object perform() {
          try {
            app.start();
          } catch (Throwable t) {
            LogsServer.instance().exception(t);
          }
          return null;
        }
      });
    }
    stage.set(App.getStage());
    view.set(App.getView());
  }
}
