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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.machinepublishers.jbrowserdriver.config.StreamHandler.Injector;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.monocle.NativePlatform;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.javafx.webkit.Accessor;

/**
 * Internal use only
 */
public class SettingsManager {
  private static final Pattern head = Pattern.compile("<head\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern html = Pattern.compile("<html\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern body = Pattern.compile("<body\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Map<Long, AtomicReference<Settings>> registry = new HashMap<Long, AtomicReference<Settings>>();
  private static final Map<HttpURLConnection, Settings> connectionSettings =
      new HashMap<HttpURLConnection, Settings>();
  private static final Object lock = new Object();
  static {
    StreamHandler.addInjector(new Injector() {
      @Override
      public byte[] inject(HttpURLConnection connection, byte[] inflatedContent) {
        Settings settings;
        synchronized (lock) {
          settings = connectionSettings.get(connection);
        }
        if (connection.getContentType() != null
            && !"false".equals(System.getProperty("jbd.quickrender"))
            && (connection.getContentType().startsWith("image/")
                || connection.getContentType().startsWith("video/")
                || connection.getContentType().startsWith("audio/")
                || connection.getContentType().startsWith("model/"))) {
          return new byte[0];
        } else if (connection.getContentType() != null
            && connection.getContentType().indexOf("text/html") > -1) {
          try {
            String charset = StreamHandler.charset(connection);
            String content = new String(inflatedContent, charset);
            Matcher matcher = head.matcher(content);
            if (matcher.find()) {
              return matcher.replaceFirst(matcher.group(0) + settings.script()).getBytes(charset);
            }
            matcher = html.matcher(content);
            if (matcher.find()) {
              return matcher.replaceFirst(
                  matcher.group(0) + "<head>" + settings.script() + "</head>").getBytes(charset);
            }
            matcher = body.matcher(content);
            if (matcher.find()) {
              return ("<html><head>" + settings.script() + "</head>"
                  + content + "</html>").getBytes(charset);
            }
            return ("<html><head>" + settings.script() + "</head><body>"
                + content + "</body></html>").getBytes(charset);
          } catch (Throwable t) {}
        }
        return null;
      }
    });
  }

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

  static LinkedHashMap<String, String> processHeaders(
      LinkedHashMap<String, String> headers, HttpURLConnection conn, boolean https) {
    final Settings settings;
    synchronized (lock) {
      settings = registry.get(Long.parseLong(headers.get("User-Agent"))).get();
      connectionSettings.put(conn, settings);
    }
    LinkedHashMap<String, String> headersIn = new LinkedHashMap<String, String>(headers);
    LinkedHashMap<String, String> headersOut = new LinkedHashMap<String, String>();
    Collection<String> names = https ? settings.headers().namesHttps() : settings.headers().namesHttp();
    for (String name : names) {
      String valueIn = headersIn.remove(name);
      String valueSettings = https ? settings.headers().headerHttps(name) : settings.headers().headerHttp(name);
      if (valueSettings == RequestHeaders.DROP_HEADER) {
        continue;
      }
      if (valueSettings == RequestHeaders.DYNAMIC_HEADER) {
        if (valueIn != null && !valueIn.isEmpty()) {
          headersOut.put(name, valueIn);
        }
      } else {
        headersOut.put(name, valueSettings);
      }
    }
    if ("no-cache".equals(headersIn.get("Cache-Control"))) {
      //JavaFX initially sets Cache-Control to no-cache but real browsers don't 
      headersIn.remove("Cache-Control");
    }
    headersOut.putAll(headersIn);
    return headersOut;
  }
}
