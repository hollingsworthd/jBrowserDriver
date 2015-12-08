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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

import javafx.application.Application;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

class SettingsManager {
  private static final Map<Long, AtomicReference<Settings>> registry = new HashMap<Long, AtomicReference<Settings>>();

  static void register(
      final AtomicReference<Stage> stage,
      final AtomicReference<WebView> view,
      final AtomicReference<Settings> settings) {
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
                    Boolean.toString(Settings.headless()),
                    Long.toString(settings.get().id()) });
          } catch (Throwable t) {
            Logs.logsFor(settings.get().id()).exception(t);
          }
        }
      }).start();
    } else {
      final App app = new App();
      app.init(
          settings.get().screenWidth(), settings.get().screenHeight(),
          Settings.headless(), settings.get().id());
      Util.exec(Pause.NONE, new AtomicInteger(-1), new Sync<Object>() {
        public Object perform() {
          try {
            app.start();
          } catch (Throwable t) {
            Logs.logsFor(settings.get().id());
          }
          return null;
        }
      }, settings.get().id());
    }
    stage.set(App.getStage());
    view.set(App.getView());

    synchronized (registry) {
      registry.put(settings.get().id(), settings);
    }
  }

  static void close(long settingsId) {
    synchronized (registry) {
      registry.remove(settingsId);
    }
  }

  static AtomicReference<Settings> get(long settingsId) {
    synchronized (registry) {
      return registry.get(settingsId);
    }
  }
}
