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
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
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
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.machinepublishers.jbrowserdriver.config.Settings;
import com.machinepublishers.jbrowserdriver.config.SettingsManager;
import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.LoadListenerClient;

public class JBrowserDriver implements Browser {
  private final AtomicReference<com.machinepublishers.jbrowserdriver.Window> window =
      new AtomicReference<com.machinepublishers.jbrowserdriver.Window>();
  private final AtomicReference<com.machinepublishers.jbrowserdriver.Navigation> navigation =
      new AtomicReference<com.machinepublishers.jbrowserdriver.Navigation>();
  private final AtomicReference<com.machinepublishers.jbrowserdriver.Options> options =
      new AtomicReference<com.machinepublishers.jbrowserdriver.Options>();
  private final AtomicReference<com.machinepublishers.jbrowserdriver.Timeouts> timeouts =
      new AtomicReference<com.machinepublishers.jbrowserdriver.Timeouts>();
  private final AtomicReference<com.machinepublishers.jbrowserdriver.TargetLocator> targetLocator =
      new AtomicReference<com.machinepublishers.jbrowserdriver.TargetLocator>();
  private final AtomicReference<Stage> stage = new AtomicReference<Stage>();
  private final AtomicReference<WebView> view = new AtomicReference<WebView>();
  private final AtomicReference<WebEngine> engine = new AtomicReference<WebEngine>();
  private final AtomicReference<Keyboard> keyboard = new AtomicReference<Keyboard>();
  private final AtomicReference<Mouse> mouse = new AtomicReference<Mouse>();
  private final AtomicReference<Capabilities> capabilities = new AtomicReference<Capabilities>();
  private final AtomicReference<Robot> robot = new AtomicReference<Robot>();
  private final AtomicInteger statusCode = new AtomicInteger();
  private final AtomicReference<Settings> settings = new AtomicReference<Settings>();
  private final AtomicBoolean initialized = new AtomicBoolean();
  private final Object initLock = new Object();
  private final AtomicBoolean pageLoaded = new AtomicBoolean();

  public JBrowserDriver() {
    this(new Settings());
  }

  public JBrowserDriver(final Settings settings) {
    this.settings.set(settings);
  }

  /**
   * Optionally call this method if you want JavaFX initialized and the browser
   * window opened immediately. Otherwise, initialization will happen lazily.
   */
  public void init() {
    if (initialized.get()) {
      return;
    }
    synchronized (initLock) {
      if (!initialized.get()) {
        SettingsManager._register(stage, view, this.settings, statusCode);
        engine.set(view.get().getEngine());
        robot.set(new Robot(stage));
        window.set(new com.machinepublishers.jbrowserdriver.Window(stage));
        timeouts.set(new com.machinepublishers.jbrowserdriver.Timeouts());
        keyboard.set(new Keyboard(robot));
        mouse.set(new Mouse(robot));
        navigation.set(new com.machinepublishers.jbrowserdriver.Navigation(
            new AtomicReference<JBrowserDriver>(this), view));
        options.set(new com.machinepublishers.jbrowserdriver.Options(window, timeouts));
        targetLocator.set(new com.machinepublishers.jbrowserdriver.TargetLocator());
        capabilities.set(new Capabilities());
        Util.exec(new Sync<Object>() {
          @Override
          public Object perform() {
            if ("true".equals(System.getProperty("jbd.trace"))) {
              Accessor.getPageFor(view.get().getEngine()).addLoadListenerClient(new LoadListenerClient() {
                private void trace(String label, int state, String url, String contentType, double progress, int errorCode) {
                  System.out.println(engine.get().getUserAgent()
                      + "-" + label + "-> " + url
                      + " ** {state: " + state
                      + ", progress: " + progress
                      + ", error: " + errorCode
                      + ", contentType: "
                      + contentType + "}");
                }

                @Override
                public void dispatchResourceLoadEvent(long frame, int state, String url, String contentType, double progress, int errorCode) {
                  trace("Rsrc", state, url, contentType, progress, errorCode);
                }

                @Override
                public void dispatchLoadEvent(long frame, int state, String url, String contentType, double progress, int errorCode) {
                  trace("Page", state, url, contentType, progress, errorCode);
                }
              });
            }
            engine.get().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
              @Override
              public void changed(ObservableValue<? extends Worker.State> observable,
                  Worker.State oldValue, Worker.State newValue) {
                if (Worker.State.SUCCEEDED.equals(newValue)
                    || Worker.State.CANCELLED.equals(newValue)
                    || Worker.State.FAILED.equals(newValue)) {
                  synchronized (pageLoaded) {
                    pageLoaded.set(true);
                    pageLoaded.notify();
                  }
                }
              }
            });
            return null;
          }
        });
        initialized.set(true);
      }
    }
  }

  @Override
  public String getPageSource() {
    init();
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<String>() {
      @Override
      public String perform() {
        return (String) view.get().getEngine().executeScript("document.documentElement.outerHTML");
      }
    });
  }

  @Override
  public String getCurrentUrl() {
    init();
    return Util.exec(new Sync<String>() {
      public String perform() {
        return view.get().getEngine().getLocation();
      }
    });
  }

  @Override
  public int getStatusCode() {
    init();
    return statusCode.get();
  }

  @Override
  public String getTitle() {
    init();
    return Util.exec(new Sync<String>() {
      public String perform() {
        return view.get().getEngine().getTitle();
      }
    });
  }

  @Override
  public void get(final String url) {
    init();
    pageLoaded.set(false);
    Util.exec(new Sync<Object>() {
      public Object perform() {
        String cleanUrl = url;
        try {
          cleanUrl = new URL(url).toExternalForm();
        } catch (Throwable t) {
          Logs.exception(t);
          cleanUrl = url.startsWith("http://") || url.startsWith("https://") ? url : "http://" + url;
        }
        engine.get().load(cleanUrl);
        return null;
      }
    });
    try {
      synchronized (pageLoaded) {
        pageLoaded.wait(timeouts.get().getPageLoadTimeoutMS());
      }
    } catch (InterruptedException e) {
      Logs.exception(e);
    }
    if (!pageLoaded.get()) {
      Util.exec(new Sync<Object>() {
        @Override
        public Object perform() {
          engine.get().getLoadWorker().cancel();
          return null;
        }
      });
    }
  }

  @Override
  public WebElement findElement(By by) {
    init();
    return by.findElement(this);
  }

  @Override
  public List<WebElement> findElements(By by) {
    init();
    return by.findElements(this);
  }

  @Override
  public WebElement findElementById(String id) {
    init();
    return Element.create(engine, robot, timeouts).findElementById(id);
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    init();
    return Element.create(engine, robot, timeouts).findElementsById(id);
  }

  @Override
  public WebElement findElementByXPath(String expr) {
    init();
    return Element.create(engine, robot, timeouts).findElementByXPath(expr);
  }

  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    init();
    return Element.create(engine, robot, timeouts).findElementsByXPath(expr);
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    init();
    return Element.create(engine, robot, timeouts).findElementByLinkText(text);
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    init();
    return Element.create(engine, robot, timeouts).findElementByPartialLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    init();
    return Element.create(engine, robot, timeouts).findElementsByLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    init();
    return Element.create(engine, robot, timeouts).findElementsByPartialLinkText(text);
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    init();
    return Element.create(engine, robot, timeouts).findElementByClassName(cssClass);
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    init();
    return Element.create(engine, robot, timeouts).findElementsByClassName(cssClass);
  }

  @Override
  public WebElement findElementByName(String name) {
    init();
    return Element.create(engine, robot, timeouts).findElementByName(name);
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    init();
    return Element.create(engine, robot, timeouts).findElementsByName(name);
  }

  @Override
  public WebElement findElementByCssSelector(String expr) {
    init();
    return Element.create(engine, robot, timeouts).findElementByCssSelector(expr);
  }

  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    init();
    return Element.create(engine, robot, timeouts).findElementsByCssSelector(expr);
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    init();
    return Element.create(engine, robot, timeouts).findElementByTagName(tagName);
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    init();
    return Element.create(engine, robot, timeouts).findElementsByTagName(tagName);
  }

  @Override
  public Object executeAsyncScript(String script, Object... args) {
    init();
    return Element.create(engine, robot, timeouts).executeAsyncScript(script, args);
  }

  @Override
  public Object executeScript(String script, Object... args) {
    init();
    return Element.create(engine, robot, timeouts).executeScript(script, args);
  }

  @Override
  public Keyboard getKeyboard() {
    init();
    return keyboard.get();
  }

  @Override
  public Mouse getMouse() {
    init();
    return mouse.get();
  }

  @Override
  public Capabilities getCapabilities() {
    init();
    return capabilities.get();
  }

  @Override
  public void close() {
    init();
    SettingsManager._deregister(settings);
    keyboard.get().sendKeys(Keys.ESCAPE);
    window.get().close();
  }

  @Override
  public String getWindowHandle() {
    init();
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<String> getWindowHandles() {
    init();
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Options manage() {
    init();
    return options.get();
  }

  @Override
  public Navigation navigate() {
    init();
    return navigation.get();
  }

  @Override
  public void quit() {
    init();
    close();
  }

  @Override
  public TargetLocator switchTo() {
    init();
    return targetLocator.get();
  }

  @Override
  public void kill() {
    init();
    close();
  }

  @Override
  public void reset() {
    init();
    // do nothing
  }

  @Override
  public Actions actions() {
    init();
    return new Actions(this);
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    init();
    BufferedImage image = Util.exec(new Sync<BufferedImage>() {
      public BufferedImage perform() {
        return SwingFXUtils.fromFXImage(view.get().snapshot(new SnapshotParameters(),
            new WritableImage((int) Math.rint(view.get().getWidth()), (int) Math.rint(view.get().getHeight()))),
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
      Util.close(out);
    }
  }
}
