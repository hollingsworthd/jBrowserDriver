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

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.openqa.selenium.Dimension;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.monocle.NativePlatform;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.javafx.webkit.Accessor;

class SettingsManager {

  private static final Map<Long, AtomicReference<Settings>> registry =
      new HashMap<Long, AtomicReference<Settings>>();

  static void register(final AtomicReference<JavaFxObject> stage, final AtomicReference<JavaFxObject> view,
      final AtomicReference<Settings> settings, final AtomicInteger statusCode) {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      public Object perform() {
        if (Settings.headless()) {
          try {
            System.setProperty("headless.geometry", settings.get().browserProperties().size().getWidth()
                + "x" + settings.get().browserProperties().size().getHeight());
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
        view.set(JavaFx.getNew(WebView.class, settings.get().id()));
        stage.set(JavaFx.getNew(Stage.class, settings.get().id()));
        AtomicReference<JavaFxObject> root = new AtomicReference<JavaFxObject>();
        root.set(JavaFx.getNew(StackPane.class, settings.get().id()));
        final Dimension size = settings.get().browserProperties().size();
        view.get().call("getEngine").call("getHistory").call("setMaxSize", 2);
        view.get().call("getEngine").call("setUserAgent", "" + settings.get().id());
        root.get().call("getChildren").call("add", view.get());
        root.get().call("setCache", false);
        stage.get().call("setScene", JavaFx.getNew(Scene.class, settings.get().id(),
            root.get().unwrap(), new Double(size.getWidth()), new Double(size.getHeight())));
        JavaFx.getStatic(Accessor.class, settings.get().id()).
            call("getPageFor", view.get().call("getEngine")).
            call("setDeveloperExtrasEnabled", false);
        stage.get().call("sizeToScene");
        stage.get().call("show");

        synchronized (registry) {
          registry.put(settings.get().id(), settings);
        }
        ProxyAuth.add(settings.get().proxy());
        addTitleListener(view, stage, settings.get().id());
        return null;
      }
    }, settings.get().id());
  }

  static void deregister(AtomicReference<Settings> settings) {
    synchronized (registry) {
      registry.remove(settings.get().id());
    }
    ProxyAuth.remove(settings.get().proxy());
    StatusMonitor.remove(settings.get().id());
    JavaFx.close(settings.get().id());
  }

  static AtomicReference<Settings> get(long settingsId) {
    synchronized (registry) {
      return registry.get(settingsId);
    }
  }

  private static void addTitleListener(final AtomicReference<JavaFxObject> view,
      final AtomicReference<JavaFxObject> stage, final long settingsId) {
    view.get().call("getEngine").call("titleProperty").
        call("addListener", JavaFx.getNew(
            DynamicTitleListener.class, settingsId, stage.get().unwrap()));
  }
}
