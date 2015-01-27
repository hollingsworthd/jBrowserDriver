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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.openqa.selenium.Dimension;

import com.machinepublishers.jbrowserdriver.Logs;
import com.machinepublishers.jbrowserdriver.Util;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.monocle.NativePlatform;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.javafx.webkit.Accessor;

/**
 * Internal use only
 */
public class SettingsManager {

  private static final Map<Long, AtomicReference<Settings>> registry = new HashMap<Long, AtomicReference<Settings>>();
  private static final Map<HttpURLConnection, AtomicReference<Settings>> connectionSettings =
      new HashMap<HttpURLConnection, AtomicReference<Settings>>();
  private static final Object lock = new Object();

  /**
   * Internal use only
   */
  public static void _register(final AtomicReference<Stage> stage, final AtomicReference<WebView> view,
      final AtomicReference<Settings> settings, final AtomicInteger statusCode) {
    Util.exec(new Sync<Object>() {
      public Object perform() {
        if (Settings.headless()) {
          try {
            System.setProperty("headless.geometry", settings.get().browserProperties().size().getWidth()
                + "x" + settings.get().browserProperties().size().getHeight());
            NativePlatform nativePlatform = NativePlatformFactory.getNativePlatform();
            Field field = NativePlatform.class.getDeclaredField("screen");
            field.setAccessible(true);
            field.set(nativePlatform, null);
            Screen.notifySettingsChanged();
          } catch (Throwable t) {
            Logs.exception(t);
          }
        }
        view.set(new WebView());
        stage.set(new Stage());
        final StackPane root = new StackPane();
        final Dimension size = settings.get().browserProperties().size();
        view.get().getEngine().getHistory().setMaxSize(2);
        view.get().getEngine().setUserAgent("" + settings.get().id());
        root.getChildren().add(view.get());
        root.setCache(false);
        stage.get().setScene(new Scene(root, size.getWidth(), size.getHeight()));
        Accessor.getPageFor(view.get().getEngine()).setDeveloperExtrasEnabled(false);
        stage.get().sizeToScene();
        stage.get().show();

        synchronized (lock) {
          registry.put(settings.get().id(), settings);
        }
        ProxyAuth.add(settings.get().proxy());
        addTitleListener(view, stage);
        addPageLoader(view, statusCode);
        return null;
      }
    });
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

  private static void addTitleListener(final AtomicReference<WebView> view, final AtomicReference<Stage> stage) {
    view.get().getEngine().titleProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable,
          String oldValue, final String newValue) {
        Util.exec(new Sync<Object>() {
          public Object perform() {
            stage.get().setTitle(newValue);
            return null;
          }
        });
      }
    });
  }

  private static void addPageLoader(final AtomicReference<WebView> view, final AtomicInteger statusCode) {
    view.get().getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
      @Override
      public void changed(final ObservableValue<? extends Worker.State> observable,
          final Worker.State oldValue, final Worker.State newValue) {
        Util.exec(new Sync<Object>() {
          public Object perform() {
            if (Worker.State.SCHEDULED.equals(newValue)) {
              view.get().setVisible(false);
              StreamHandler.startStatusMonitor();
            } else if (Worker.State.SUCCEEDED.equals(newValue)
                || Worker.State.CANCELLED.equals(newValue)
                || Worker.State.FAILED.equals(newValue)) {
              int code = StreamHandler.stopStatusMonitor(view.get().getEngine().getLocation());
              view.get().setVisible(true);
              statusCode.set(Worker.State.SUCCEEDED.equals(newValue) ? code : 499);
            }
            return null;
          }
        });
      }
    });
  }
}
