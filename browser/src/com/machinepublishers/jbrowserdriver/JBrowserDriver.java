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

public class JBrowserDriver implements Browser {
  private final BrowserContext context = new BrowserContext();

  public JBrowserDriver() {
    this(new Settings());
  }

  public JBrowserDriver(final Settings settings) {
    context.current().settings.set(settings);
  }

  /**
   * Optionally call this method if you want JavaFX initialized and the browser
   * window opened immediately. Otherwise, initialization will happen lazily.
   */
  public void init() {
    context.current().init(this, context);
  }

  @Override
  public String getPageSource() {
    init();
    return Util.exec(context.current().timeouts.get().getScriptTimeoutMS(), new Sync<String>() {
      @Override
      public String perform() {
        return context.current().view.get().call("getEngine").
            call("executeScript", "document.documentElement.outerHTML").toString();
      }
    }, context.current().settingsId.get());
  }

  @Override
  public String getCurrentUrl() {
    init();
    return Util.exec(new Sync<String>() {
      public String perform() {
        return context.current().view.get().call("getEngine").call("getLocation").toString();
      }
    }, context.current().settingsId.get());
  }

  @Override
  public int getStatusCode() {
    init();
    return context.current().statusCode.get();
  }

  @Override
  public String getTitle() {
    init();
    return Util.exec(new Sync<String>() {
      public String perform() {
        return context.current().view.get().call("getEngine").call("getTitle").toString();
      }
    }, context.current().settingsId.get());
  }

  @Override
  public void get(final String url) {
    init();
    context.current().pageLoaded.set(false);
    Util.exec(new Sync<Object>() {
      public Object perform() {
        String cleanUrl = url;
        try {
          cleanUrl = new URL(url).toExternalForm();
        } catch (Throwable t) {
          Logs.exception(t);
          cleanUrl = url.startsWith("http://") || url.startsWith("https://") ? url : "http://" + url;
        }
        context.current().engine.get().call("load", cleanUrl);
        return null;
      }
    }, context.current().settingsId.get());
    try {
      synchronized (context.current().pageLoaded) {
        if (!context.current().pageLoaded.get()) {
          context.current().pageLoaded.wait(context.current().timeouts.get().getPageLoadTimeoutMS());
        }
      }
    } catch (InterruptedException e) {
      Logs.exception(e);
    }
    if (!context.current().pageLoaded.get()) {
      Util.exec(new Sync<Object>() {
        @Override
        public Object perform() {
          context.current().engine.get().call("getLoadWorker").call("cancel");
          return null;
        }
      }, context.current().settingsId.get());
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
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementById(id);
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementsById(id);
  }

  @Override
  public WebElement findElementByXPath(String expr) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementByXPath(expr);
  }

  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementsByXPath(expr);
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementByLinkText(text);
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementByPartialLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementsByLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementsByPartialLinkText(text);
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementByClassName(cssClass);
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementsByClassName(cssClass);
  }

  @Override
  public WebElement findElementByName(String name) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementByName(name);
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementsByName(name);
  }

  @Override
  public WebElement findElementByCssSelector(String expr) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementByCssSelector(expr);
  }

  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementsByCssSelector(expr);
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementByTagName(tagName);
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).findElementsByTagName(tagName);
  }

  @Override
  public Object executeAsyncScript(String script, Object... args) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).executeAsyncScript(script, args);
  }

  @Override
  public Object executeScript(String script, Object... args) {
    init();
    return Element.create(context.current().engine,
        context.current().robot, context.current().timeouts).executeScript(script, args);
  }

  @Override
  public Keyboard getKeyboard() {
    init();
    return context.current().keyboard.get();
  }

  @Override
  public Mouse getMouse() {
    init();
    return context.current().mouse.get();
  }

  @Override
  public Capabilities getCapabilities() {
    init();
    return context.current().capabilities.get();
  }

  @Override
  public void close() {
    init();
    SettingsManager._deregister(context.current().settings);
    context.current().keyboard.get().sendKeys(Keys.ESCAPE);
    context.current().window.get().close();
  }

  @Override
  public String getWindowHandle() {
    init();
    return context.currentId();
  }

  @Override
  public Set<String> getWindowHandles() {
    init();
    return context.ids();
  }

  @Override
  public Options manage() {
    init();
    return context.current().options.get();
  }

  @Override
  public Navigation navigate() {
    init();
    return context.current().navigation.get();
  }

  @Override
  public void quit() {
    init();
    close();
  }

  @Override
  public TargetLocator switchTo() {
    init();
    return context.current().targetLocator.get();
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
            SwingFXUtils.class, Long.parseLong(context.current().engine.get().call("getUserAgent").toString())).
            call("fromFXImage", context.current().view.get().
                call("snapshot", JavaFx.getNew(SnapshotParameters.class, context.current().settingsId.get()),
                    JavaFx.getNew(WritableImage.class, context.current().settingsId.get(),
                        (int) Math.rint((Double) context.current().view.get().call("getWidth").unwrap()),
                        (int) Math.rint((Double) context.current().view.get().call("getHeight").unwrap()))), null);
      }
    }, context.current().settingsId.get());
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      JavaFx.getStatic(ImageIO.class, context.current().settingsId.get()).call("write", image, "png", out);
      return outputType.convertFromPngBytes(out.toByteArray());
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    } finally {
      Util.close(out);
    }
  }
}
