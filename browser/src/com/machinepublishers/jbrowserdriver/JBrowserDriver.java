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
package com.machinepublishers.jbrowserdriver;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import com.machinepublishers.browser.Browser;
import com.machinepublishers.jbrowserdriver.Util.Async;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.machinepublishers.jbrowserdriver.config.StreamHandler;
import com.machinepublishers.jbrowserdriver.config.StreamHandler.Injector;
import com.sun.javafx.webkit.Accessor;

public class JBrowserDriver implements Browser {
  private static final Random rand = new Random();
  private static final CookieManager cookieManager = new CookieManager();
  private static final AtomicLong settingsId = new AtomicLong();
  private final long mySettingsId;
  static {
    if (!"true".equals(System.getProperty("browsergui"))) {
      System.setProperty("glass.platform", "Monocle");
      System.setProperty("monocle.platform", "Headless");
      System.setProperty("prism.order", "sw");
      //AWT gui is never used anyhow, but set this to prevent programmer errors
      System.setProperty("java.awt.headless", "true");
    }
    StreamHandler.init();
    CookieHandler.setDefault(cookieManager);
    ResponseCache.setDefault(new ResponseCache() {
      @Override
      public CacheRequest put(URI arg0, URLConnection arg1) throws IOException {
        return null;
      }

      @Override
      public CacheResponse get(URI arg0, String arg1, Map<String, List<String>> arg2) throws IOException {
        return null;
      }
    });
  }
  private final com.machinepublishers.jbrowserdriver.Window window;
  private final WebEngine engine;
  private final WebView view;
  private final Keyboard keyboard;
  private final Mouse mouse;
  private final Navigation navigation;
  private final Capabilities capabilities;
  private final Options options;
  private final com.machinepublishers.jbrowserdriver.Timeouts timeouts;
  private final TargetLocator targetLocator;
  private final Robot robot;
  private final AtomicInteger statusCode = new AtomicInteger();

  public JBrowserDriver() {
    this(new Settings());
  }

  public JBrowserDriver(final Settings settings) {
    mySettingsId = settingsId.incrementAndGet();
    final Map<String, Object> objects = new HashMap<String, Object>();
    final Object lock = new Object();
    Util.exec(new Sync<Object>() {
      public Object perform() {
        synchronized (lock) {
          final Stage stage = new Stage();
          final WebView view = new WebView();
          final WebEngine engine = view.getEngine();
          StringBuilder scriptBuilder = new StringBuilder();
          String id = "A" + rand.nextLong();
          scriptBuilder.append("<script id='" + id + "' language='javascript'>");
          scriptBuilder.append("try{");
          scriptBuilder.append(settings.browserTimeZone().script());
          scriptBuilder.append(settings.browserProperties().script());
          scriptBuilder.append("}catch(e){}");
          scriptBuilder.append("document.getElementsByTagName('head')[0].removeChild(document.getElementById('" + id + "'));");
          scriptBuilder.append("</script>");
          final String script = scriptBuilder.toString();
          StreamHandler.addInjector(new Injector() {
            @Override
            public byte[] inject(HttpURLConnection connection, byte[] inflatedContent) {
              if (connection.getContentType().indexOf("text/html") > -1) {
                try {
                  String charset = StreamHandler.charset(connection);
                  String content = new String(inflatedContent, charset);
                  Pattern head = Pattern.compile("<head\\b[^>]*>");
                  Matcher matcher = head.matcher(content);
                  if (matcher.find()) {
                    return matcher.replaceFirst(matcher.group(0) + script).getBytes(charset);
                  }
                } catch (Throwable t) {}
              }
              return null;
            }
          });
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
          engine.getHistory().setMaxSize(2);
          engine.setUserAgent("" + mySettingsId);
          Settings.register(mySettingsId, settings);
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
          final StackPane root = new StackPane();
          root.getChildren().add(view);
          root.setCache(false);
          final Scene scene = new Scene(root, 800, 600);
          stage.setScene(scene);
          Accessor.getPageFor(engine).setDeveloperExtrasEnabled(false);
          Accessor.getPageFor(engine).setUsePageCache(false);
          stage.sizeToScene();
          stage.show();
          final Robot robot = new Robot(stage);
          objects.put("stage", stage);
          objects.put("view", view);
          objects.put("engine", engine);
          objects.put("robot", robot);
        }
        return null;
      }
    });
    synchronized (lock) {
      engine = (WebEngine) objects.get("engine");
      view = (WebView) objects.get("view");
      robot = (Robot) objects.get("robot");
      window = new com.machinepublishers.jbrowserdriver.Window((Stage) objects.get("stage"));
      keyboard = new Keyboard(robot);
      mouse = new Mouse(robot);
      navigation = new com.machinepublishers.jbrowserdriver.Navigation(this, engine);
      capabilities = new Capabilities();
      timeouts = new com.machinepublishers.jbrowserdriver.Timeouts();
      options = new com.machinepublishers.jbrowserdriver.Options(window, timeouts, cookieManager);
      targetLocator = new com.machinepublishers.jbrowserdriver.TargetLocator();
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    Settings.deregister(mySettingsId);
  }

  @Override
  public String getPageSource() {
    return Util.exec(timeouts.getScriptTimeoutMS(), new Sync<String>() {
      @Override
      public String perform() {
        return (String) engine.executeScript("document.documentElement.outerHTML");
      }
    });
  }

  @Override
  public String getCurrentUrl() {
    return Util.exec(new Sync<String>() {
      public String perform() {
        return engine.getLocation();
      }
    });
  }

  @Override
  public int getStatusCode() {
    return statusCode.get();
  }

  @Override
  public String getTitle() {
    return Util.exec(new Sync<String>() {
      public String perform() {
        return engine.getTitle();
      }
    });
  }

  @Override
  public void get(final String url) {
    final boolean[] lock = new boolean[1];
    final ChangeListener<Worker.State> changeListener = new ChangeListener<Worker.State>() {
      @Override
      public void changed(ObservableValue<? extends Worker.State> observable,
          Worker.State oldValue, Worker.State newValue) {
        if (Worker.State.SUCCEEDED.equals(newValue)
            || Worker.State.CANCELLED.equals(newValue)
            || Worker.State.FAILED.equals(newValue)) {
          synchronized (lock) {
            lock[0] = true;
            lock.notify();
          }
        }
      }
    };
    Util.exec(new Async() {
      public void perform() {
        engine.getLoadWorker().stateProperty().addListener(changeListener);
        String cleanUrl = url;
        try {
          cleanUrl = new URL(url).toExternalForm();
        } catch (Throwable t) {
          Logs.exception(t);
          cleanUrl = url.startsWith("http://") || url.startsWith("https://") ? url : "http://" + url;
        }
        engine.load(cleanUrl);
      }
    });
    synchronized (lock) {
      if (!lock[0]) {
        try {
          lock.wait(timeouts.getPageLoadTimeoutMS());
        } catch (InterruptedException e) {
          Logs.exception(e);
        }
      }
    }
    Util.exec(new Async() {
      public void perform() {
        engine.getLoadWorker().stateProperty().removeListener(changeListener);
      }
    });
  }

  @Override
  public WebElement findElement(By by) {
    return by.findElement(this);
  }

  @Override
  public List<WebElement> findElements(By by) {
    return by.findElements(this);
  }

  @Override
  public WebElement findElementById(String id) {
    return new Element(engine, robot, timeouts).findElementById(id);
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    return new Element(engine, robot, timeouts).findElementsById(id);
  }

  @Override
  public WebElement findElementByXPath(String expr) {
    return new Element(engine, robot, timeouts).findElementByXPath(expr);
  }

  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    return new Element(engine, robot, timeouts).findElementsByXPath(expr);
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    return new Element(engine, robot, timeouts).findElementByLinkText(text);
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    return new Element(engine, robot, timeouts).findElementByPartialLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    return new Element(engine, robot, timeouts).findElementsByLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    return new Element(engine, robot, timeouts).findElementsByPartialLinkText(text);
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    return new Element(engine, robot, timeouts).findElementByClassName(cssClass);
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    return new Element(engine, robot, timeouts).findElementsByClassName(cssClass);
  }

  @Override
  public WebElement findElementByName(String name) {
    return new Element(engine, robot, timeouts).findElementByName(name);
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    return new Element(engine, robot, timeouts).findElementsByName(name);
  }

  @Override
  public WebElement findElementByCssSelector(String expr) {
    return new Element(engine, robot, timeouts).findElementByCssSelector(expr);
  }

  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    return new Element(engine, robot, timeouts).findElementsByCssSelector(expr);
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    return new Element(engine, robot, timeouts).findElementByTagName(tagName);
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    return new Element(engine, robot, timeouts).findElementsByTagName(tagName);
  }

  @Override
  public Object executeAsyncScript(String script, Object... args) {
    return new Element(engine, robot, timeouts).executeAsyncScript(script, args);
  }

  @Override
  public Object executeScript(String script, Object... args) {
    return new Element(engine, robot, timeouts).executeScript(script, args);
  }

  @Override
  public Keyboard getKeyboard() {
    return keyboard;
  }

  @Override
  public Mouse getMouse() {
    return mouse;
  }

  @Override
  public Capabilities getCapabilities() {
    return capabilities;
  }

  @Override
  public void close() {
    keyboard.sendKeys(Keys.ESCAPE);
    window.close();
  }

  @Override
  public String getWindowHandle() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<String> getWindowHandles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Options manage() {
    return options;
  }

  @Override
  public Navigation navigate() {
    return navigation;
  }

  @Override
  public void quit() {
    close();
  }

  @Override
  public TargetLocator switchTo() {
    return targetLocator;
  }

  @Override
  public void kill() {
    close();
  }

  @Override
  public void reset() {
    // do nothing
  }

  @Override
  public Actions actions() {
    return new Actions(this);
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    BufferedImage image = Util.exec(new Sync<BufferedImage>() {
      public BufferedImage perform() {
        return SwingFXUtils.fromFXImage(view.snapshot(new SnapshotParameters(),
            new WritableImage((int) Math.rint(view.getWidth()), (int) Math.rint(view.getHeight()))),
            null);
      }
    });
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      ImageIO.write(image, "png", out);
      return outputType.convertFromPngBytes(out.toByteArray());
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    } finally {
      try {
        out.close();
      } catch (Throwable t) {
        Logs.exception(t);
      }
    }
  }
}
