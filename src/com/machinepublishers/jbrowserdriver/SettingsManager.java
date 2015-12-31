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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

import javafx.application.Application;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

class SettingsManager {
  private static final AtomicReference<Settings> settings = new AtomicReference<Settings>();

  static Settings settings() {
    return settings.get();
  }

  static void register(final Settings settings) {
    SettingsManager.settings.set(settings);
  }

  static void register(
      final AtomicReference<Stage> stage,
      final AtomicReference<WebView> view) {
    ProxyAuth.add(settings.get().proxy());
    if (Settings.headless() &&
        com.sun.glass.ui.Application.GetApplication() == null) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            Application.launch(App.class,
                new String[] {
                    Integer.toString(settings.get().screenWidth()),
                    Integer.toString(settings.get().screenHeight()),
                    Boolean.toString(Settings.headless()) });
          } catch (Throwable t) {
            LogsServer.instance().exception(t);
          }
        }
      }).start();
    } else {
      final App app = new App();
      app.init(
          settings.get().screenWidth(), settings.get().screenHeight(),
          Settings.headless());
      Util.exec(Pause.NONE, new AtomicInteger(-1), new Sync<Object>() {
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
