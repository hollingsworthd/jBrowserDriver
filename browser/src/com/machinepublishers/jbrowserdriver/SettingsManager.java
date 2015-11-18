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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import javafx.application.Platform;

import org.openqa.selenium.Dimension;

import com.sun.glass.ui.Screen;
import com.sun.glass.ui.monocle.NativePlatform;
import com.sun.glass.ui.monocle.NativePlatformFactory;

class SettingsManager {
  private static final Map<Long, AtomicReference<Settings>> registry =
      new HashMap<Long, AtomicReference<Settings>>();

  static void register(
      final AtomicReference<JavaFxObject> stage,
      final AtomicReference<JavaFxObject> view,
      final AtomicReference<Thread> appThread,
      final AtomicReference<Settings> settings,
      final AtomicInteger statusCode) {
    if (Settings.headless()) {
      try {
        System.setProperty("headless.geometry", settings.get().screen().getWidth()
            + "x" + settings.get().screen().getHeight());
        JavaFxObject nativePlatform = JavaFx.getStatic(NativePlatformFactory.class,
            settings.get().id()).call("getNativePlatform");
        Field field = ((Class) JavaFx.getStatic(NativePlatform.class,
            settings.get().id()).unwrap()).getDeclaredField("screen");
        field.setAccessible(true);
        field.set(nativePlatform.unwrap(), null);
        JavaFx.getStatic(Screen.class, settings.get().id()).call("notifySettingsChanged");
      } catch (Throwable t) {
        Logs.exception(t);
      }
    }
    ProxyAuth.add(settings.get().proxy());
    appThread.set(new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Dimension size = settings.get().screen();
          JavaFx.getStatic(Application.class, settings.get().id()).call(
              "launch", JavaFx.getStatic(DynamicApplication.class, settings.get().id()),
              new String[] { Integer.toString(size.getWidth()), Integer.toString(size.getHeight()),
                  Boolean.toString(Settings.headless()), Long.toString(settings.get().id()) });
        } catch (Throwable t) {
          Logs.exception(t);
        }
      }
    }));
    appThread.get().start();
    stage.set(JavaFx.getStatic(DynamicApplication.class, settings.get().id()).call("getStage"));
    view.set(JavaFx.getStatic(DynamicApplication.class, settings.get().id()).call("getView"));

    synchronized (registry) {
      registry.put(settings.get().id(), settings);
    }
  }

  static void deregister(AtomicReference<Settings> settings, BrowserContext context) {
    synchronized (registry) {
      registry.remove(settings.get().id());
    }
    if (Settings.headless()) {
      JavaFx.getStatic(Platform.class, settings.get().id()).call("exit");
    }
    StatusMonitor.get(settings.get().id()).clearStatusMonitor();
    StatusMonitor.remove(settings.get().id());
    context.settings.get().cookieStore().clear();
    ProxyAuth.remove(settings.get().proxy());
    JavaFx.close(settings.get().id());
    StreamConnection.shutDown();

    for (BrowserContextItem item : context.items()) {
      Thread thread = item.appThread.get();
      if (thread != null) {
        while (true) {
          try {
            thread.join();
            break;
          } catch (InterruptedException e) {}
        }
      }
    }
  }

  static AtomicReference<Settings> get(long settingsId) {
    synchronized (registry) {
      return registry.get(settingsId);
    }
  }
}
