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

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import com.machinepublishers.browser.Browser;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.machinepublishers.jbrowserdriver.config.JavaFx;
import com.machinepublishers.jbrowserdriver.config.JavaFxObject;
import com.machinepublishers.jbrowserdriver.config.Settings;
import com.machinepublishers.jbrowserdriver.config.SettingsManager;
import com.sun.javafx.webkit.Accessor;

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
  private final AtomicReference<JavaFxObject> stage = new AtomicReference<JavaFxObject>();
  private final AtomicReference<JavaFxObject> view = new AtomicReference<JavaFxObject>();
  private final AtomicReference<JavaFxObject> engine = new AtomicReference<JavaFxObject>();
  private final AtomicReference<Keyboard> keyboard = new AtomicReference<Keyboard>();
  private final AtomicReference<Mouse> mouse = new AtomicReference<Mouse>();
  private final AtomicReference<Capabilities> capabilities = new AtomicReference<Capabilities>();
  private final AtomicReference<Robot> robot = new AtomicReference<Robot>();
  private final AtomicInteger statusCode = new AtomicInteger();
  private final AtomicReference<Settings> settings = new AtomicReference<Settings>();
  private final AtomicBoolean initialized = new AtomicBoolean();
  private final Object initLock = new Object();
  private final AtomicBoolean pageLoaded = new AtomicBoolean();
  private final AtomicLong settingsId = new AtomicLong();

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
        engine.set(view.get().call("getEngine"));
        settingsId.set(Long.parseLong(engine.get().call("getUserAgent").toString()));
        robot.set(new Robot(stage, settingsId.get()));
        window.set(new com.machinepublishers.jbrowserdriver.Window(stage, settingsId.get()));
        timeouts.set(new com.machinepublishers.jbrowserdriver.Timeouts());
        keyboard.set(new Keyboard(robot));
        mouse.set(new Mouse(robot));
        navigation.set(new com.machinepublishers.jbrowserdriver.Navigation(
            new AtomicReference<JBrowserDriver>(this), view, settingsId.get()));
        options.set(new com.machinepublishers.jbrowserdriver.Options(window, timeouts));
        targetLocator.set(new com.machinepublishers.jbrowserdriver.TargetLocator());
        capabilities.set(new Capabilities());
        final boolean trace = "true".equals(System.getProperty("jbd.trace"));
        Util.exec(new Sync<Object>() {
          @Override
          public Object perform() {
            JavaFx.getStatic(Accessor.class, settingsId.get()).
                call("getPageFor", view.get().call("getEngine")).
                call("addLoadListenerClient",
                    JavaFx.getNew(DynamicHttpLog.class, settingsId.get(), trace, settingsId.get()));
            engine.get().call("getLoadWorker").call("stateProperty").call("addListener",
                JavaFx.getNew(DynamicStateListener.class, settingsId.get(), pageLoaded));
            return null;
          }
        }, settingsId.get());
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
        return view.get().call("getEngine").
            call("executeScript", "document.documentElement.outerHTML").toString();
      }
    }, settingsId.get());
  }

  @Override
  public String getCurrentUrl() {
    init();
    return Util.exec(new Sync<String>() {
      public String perform() {
        return view.get().call("getEngine").call("getLocation").toString();
      }
    }, settingsId.get());
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
        return view.get().call("getEngine").call("getTitle").toString();
      }
    }, settingsId.get());
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
        engine.get().call("load", cleanUrl);
        return null;
      }
    }, settingsId.get());
    try {
      synchronized (pageLoaded) {
        if (!pageLoaded.get()) {
          pageLoaded.wait(timeouts.get().getPageLoadTimeoutMS());
        }
      }
    } catch (InterruptedException e) {
      Logs.exception(e);
    }
    if (!pageLoaded.get()) {
      Util.exec(new Sync<Object>() {
        @Override
        public Object perform() {
          engine.get().call("getLoadWorker").call("cancel");
          return null;
        }
      }, settingsId.get());
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
    JavaFxObject image = Util.exec(new Sync<JavaFxObject>() {
      public JavaFxObject perform() {
        return JavaFx.getStatic(
            SwingFXUtils.class, Long.parseLong(engine.get().call("getUserAgent").toString())).
            call("fromFXImage", view.get().
                call("snapshot", JavaFx.getNew(SnapshotParameters.class, settingsId.get()),
                    JavaFx.getNew(WritableImage.class, settingsId.get(),
                        (int) Math.rint((Double) view.get().call("getWidth").unwrap()),
                        (int) Math.rint((Double) view.get().call("getHeight").unwrap()))), null);
      }
    }, settingsId.get());
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      JavaFx.getStatic(ImageIO.class, settingsId.get()).call("write", image, "png", out);
      return outputType.convertFromPngBytes(out.toByteArray());
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    } finally {
      Util.close(out);
    }
  }
}
