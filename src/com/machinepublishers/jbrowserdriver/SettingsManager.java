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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.Dimension;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

import javafx.application.Application;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class SettingsManager {
  private static final Map<Long, AtomicReference<Settings>> registry = new HashMap<Long, AtomicReference<Settings>>();

  public static void register(
      final AtomicReference<Stage> stage,
      final AtomicReference<WebView> view,
      final AtomicReference<Settings> settings) {
    ProxyAuth.add(settings.get().proxy());
    if (Settings.headless() &&
        com.sun.glass.ui.Application.GetApplication() == null) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          Dimension size = settings.get().screen();
          try {
            Application.launch(BrowserApp.class,
                new String[] { Integer.toString(size.getWidth()), Integer.toString(size.getHeight()),
                    Boolean.toString(Settings.headless()), Long.toString(settings.get().id()) });
          } catch (Throwable t) {
            Logs.logsFor(settings.get().id()).exception(t);
          }
        }
      }).start();
    } else {
      Dimension size = settings.get().screen();
      final BrowserApp app = new BrowserApp();
      app.init(
          size.getWidth(), size.getHeight(), Settings.headless(), settings.get().id());
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
    stage.set(BrowserApp.getStage());
    view.set(BrowserApp.getView());

    synchronized (registry) {
      registry.put(settings.get().id(), settings);
    }
  }

  public static void close(long settingsId) {
    synchronized (registry) {
      registry.remove(settingsId);
    }
  }

  public static AtomicReference<Settings> get(long settingsId) {
    synchronized (registry) {
      return registry.get(settingsId);
    }
  }
}
