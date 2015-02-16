/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver.config;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.openqa.selenium.Dimension;

import com.machinepublishers.jbrowserdriver.Logs;
import com.machinepublishers.jbrowserdriver.Util;
import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.monocle.NativePlatform;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.javafx.webkit.Accessor;

/**
 * Internal use only
 */
public class SettingsManager {

  private static final Map<Long, AtomicReference<Settings>> registry =
      new HashMap<Long, AtomicReference<Settings>>();
  private static final Map<HttpURLConnection, AtomicReference<Settings>> connectionSettings =
      new HashMap<HttpURLConnection, AtomicReference<Settings>>();
  private static final Object lock = new Object();

  /**
   * Internal use only
   */
  public static void _register(final AtomicReference<JavaFxObject> stage, final AtomicReference<JavaFxObject> view,
      final AtomicReference<Settings> settings, final AtomicInteger statusCode) {
    Util.exec(Pause.NONE, new Sync<Object>() {
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

        synchronized (lock) {
          registry.put(settings.get().id(), settings);
        }
        ProxyAuth.add(settings.get().proxy());
        addTitleListener(view, stage, settings.get().id());
        return null;
      }
    }, settings.get().id());
  }

  /**
   * Internal use only
   */
  public static void _deregister(AtomicReference<Settings> settings) {
    synchronized (lock) {
      registry.remove(settings.get().id());
    }
    ProxyAuth.remove(settings.get().proxy());
  }

  static AtomicReference<Settings> get(String settingsId) {
    synchronized (lock) {
      return registry.get(Long.parseLong(settingsId));
    }
  }

  static AtomicReference<Settings> get(HttpURLConnection connection) {
    synchronized (lock) {
      return connectionSettings.get(connection);
    }
  }

  static AtomicReference<Settings> store(String settingsId, HttpURLConnection conn) {
    synchronized (lock) {
      AtomicReference<Settings> settings = registry.get(Long.parseLong(settingsId));
      connectionSettings.put(conn, settings);
      return settings;
    }
  }

  static void clearConnections() {
    synchronized (lock) {
      connectionSettings.clear();
    }
  }

  private static void addTitleListener(final AtomicReference<JavaFxObject> view,
      final AtomicReference<JavaFxObject> stage, final long settingsId) {
    view.get().call("getEngine").call("titleProperty").
        call("addListener", JavaFx.getNew(
            DynamicTitleListener.class, settingsId, stage.get().unwrap()));
  }
}
