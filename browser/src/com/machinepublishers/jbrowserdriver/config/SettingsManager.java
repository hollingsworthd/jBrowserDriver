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

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.openqa.selenium.Dimension;

import com.machinepublishers.jbrowserdriver.Util;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.machinepublishers.jbrowserdriver.config.StreamHandler.Injector;
import com.sun.javafx.webkit.Accessor;

/**
 * Internal use only
 */
public class SettingsManager {
  private static final Random rand = new Random();
  private static final Pattern head = Pattern.compile("<head\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern html = Pattern.compile("<html\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Pattern body = Pattern.compile("<body\\b[^>]*>", Pattern.CASE_INSENSITIVE);
  private static final Map<Long, Settings> registry = new HashMap<Long, Settings>();
  private static final Object lock = new Object();

  /**
   * Internal use only
   */
  public static synchronized void _register(final Stage stage, final WebView view,
      final Settings settings, final AtomicInteger statusCode) {
    Util.exec(new Sync<Object>() {
      public Object perform() {
        final WebEngine engine = view.getEngine();
        final StackPane root = new StackPane();
        final Dimension size = settings.browserProperties().size();
        engine.getHistory().setMaxSize(2);
        engine.setUserAgent("" + settings.id());
        root.getChildren().add(view);
        root.setCache(false);
        stage.setScene(new Scene(root, size.getWidth(), size.getHeight()));
        Accessor.getPageFor(engine).setDeveloperExtrasEnabled(false);
        Accessor.getPageFor(engine).setUsePageCache(false);
        stage.sizeToScene();
        stage.show();

        synchronized (lock) {
          registry.put(settings.id(), settings);
        }
        addTitleListener(engine, stage);
        addPageLoader(engine, view, statusCode);
        addInjector(settings);
        return null;
      }
    });
  }

  /**
   * Internal use only
   */
  public static void _deregister(Settings settings) {
    synchronized (lock) {
      registry.remove(settings.id());
    }
  }

  static Settings get(Long id) {
    synchronized (lock) {
      return registry.get(id);
    }
  }

  private static void addInjector(Settings settings) {
    StringBuilder scriptBuilder = new StringBuilder();
    String id = "A" + rand.nextLong();
    scriptBuilder.append("<script id='" + id + "' language='javascript'>");
    scriptBuilder.append("try{");
    scriptBuilder.append(settings.browserTimeZone().script());
    scriptBuilder.append(settings.browserProperties().script());
    scriptBuilder.append("}catch(e){}");
    scriptBuilder.append("document.getElementsByTagName('head')[0].removeChild("
        + "document.getElementById('" + id + "'));");
    scriptBuilder.append("</script>");
    final String script = scriptBuilder.toString();
    StreamHandler.addInjector(new Injector() {
      @Override
      public byte[] inject(HttpURLConnection connection, byte[] inflatedContent) {
        if (connection.getContentType().indexOf("text/html") > -1) {
          try {
            String charset = StreamHandler.charset(connection);
            String content = new String(inflatedContent, charset);
            Matcher matcher = head.matcher(content);
            if (matcher.find()) {
              return matcher.replaceFirst(matcher.group(0) + script).getBytes(charset);
            }
            matcher = html.matcher(content);
            if (matcher.find()) {
              return matcher.replaceFirst(matcher.group(0) + "<head>" + script + "</head>").getBytes(charset);
            }
            matcher = body.matcher(content);
            if (matcher.find()) {
              return ("<html><head>" + script + "</head>" + content + "</html>").getBytes(charset);
            }
            return ("<html><head>" + script + "</head><body>" + content + "</body></html>").getBytes(charset);
          } catch (Throwable t) {}
        }
        return null;
      }
    });
  }

  private static void addTitleListener(final WebEngine engine, final Stage stage) {
    engine.titleProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable,
          String oldValue, final String newValue) {
        Util.exec(new Sync<Object>() {
          public Object perform() {
            stage.setTitle(newValue);
            return null;
          }
        });
      }
    });
  }

  private static void addPageLoader(final WebEngine engine,
      final WebView view, final AtomicInteger statusCode) {
    engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
      @Override
      public void changed(final ObservableValue<? extends Worker.State> observable,
          final Worker.State oldValue, final Worker.State newValue) {
        Util.exec(new Sync<Object>() {
          public Object perform() {
            if (Worker.State.SCHEDULED.equals(newValue)) {
              view.setVisible(false);
              StreamHandler.startStatusMonitor();
            } else if (Worker.State.SUCCEEDED.equals(newValue)
                || Worker.State.CANCELLED.equals(newValue)
                || Worker.State.FAILED.equals(newValue)) {
              int code = StreamHandler.stopStatusMonitor(engine.getLocation());
              view.setVisible(true);
              statusCode.set(Worker.State.SUCCEEDED.equals(newValue) ? code : 499);
            }
            return null;
          }
        });
      }
    });
  }

}
