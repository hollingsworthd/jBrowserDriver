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
import java.util.concurrent.atomic.AtomicReference;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import com.machinepublishers.browser.Browser;
import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

public class JBrowserDriver implements Browser {
  private final BrowserContext context = new BrowserContext();
  private final AtomicReference<Settings> settings = new AtomicReference<Settings>();

  public JBrowserDriver() {
    this(new Settings());
  }

  public JBrowserDriver(final Settings settings) {
    this.settings.set(new Settings(settings));
    context.item().settings.set(this.settings.get());
  }

  /**
   * Optionally call this method if you want JavaFX initialized and the browser
   * window opened immediately. Otherwise, initialization will happen lazily.
   */
  public void init() {
    context.init(this);
  }

  @Override
  public String getPageSource() {
    init();
    return Util.exec(Pause.NONE, context.item().statusCode, context.item().timeouts.get().getScriptTimeoutMS(),
        new Sync<String>() {
          @Override
          public String perform() {
            return context.item().view.get().call("getEngine").
                call("executeScript", "document.documentElement.outerHTML").toString();
          }
        }, context.item().settingsId.get());
  }

  @Override
  public String getCurrentUrl() {
    init();
    return Util.exec(Pause.NONE, context.item().statusCode, new Sync<String>() {
      public String perform() {
        return context.item().view.get().call("getEngine").call("getLocation").toString();
      }
    }, context.item().settingsId.get());
  }

  @Override
  public int getStatusCode() {
    init();
    return context.item().statusCode.get();
  }

  @Override
  public String getTitle() {
    init();
    return Util.exec(Pause.NONE, context.item().statusCode, new Sync<String>() {
      public String perform() {
        return context.item().view.get().call("getEngine").call("getTitle").toString();
      }
    }, context.item().settingsId.get());
  }

  @Override
  public void get(final String url) {
    init();
    context.item().statusCode.set(0);
    Util.exec(Pause.SHORT, new Sync<Object>() {
      public Object perform() {
        String cleanUrl = url;
        try {
          cleanUrl = new URL(url).toExternalForm();
        } catch (Throwable t) {
          Logs.exception(t);
          cleanUrl = url.startsWith("http://") || url.startsWith("https://") ? url : "http://" + url;
        }
        context.item().engine.get().call("load", cleanUrl);
        return null;
      }
    }, context.item().settingsId.get());
    try {
      synchronized (context.item().statusCode) {
        if (context.item().statusCode.get() == 0) {
          context.item().statusCode.wait(context.item().timeouts.get().getPageLoadTimeoutMS());
        }
      }
    } catch (InterruptedException e) {
      Logs.exception(e);
    }
    if (context.item().statusCode.get() == 0) {
      Util.exec(Pause.SHORT, new Sync<Object>() {
        @Override
        public Object perform() {
          context.item().engine.get().call("getLoadWorker").call("cancel");
          return null;
        }
      }, context.item().settingsId.get());
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
    return Element.create(context).findElementById(id);
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    init();
    return Element.create(context).findElementsById(id);
  }

  @Override
  public WebElement findElementByXPath(String expr) {
    init();
    return Element.create(context).findElementByXPath(expr);
  }

  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    init();
    return Element.create(context).findElementsByXPath(expr);
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    init();
    return Element.create(context).findElementByLinkText(text);
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    init();
    return Element.create(context).findElementByPartialLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    init();
    return Element.create(context).findElementsByLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    init();
    return Element.create(context).findElementsByPartialLinkText(text);
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    init();
    return Element.create(context).findElementByClassName(cssClass);
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    init();
    return Element.create(context).findElementsByClassName(cssClass);
  }

  @Override
  public WebElement findElementByName(String name) {
    init();
    return Element.create(context).findElementByName(name);
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    init();
    return Element.create(context).findElementsByName(name);
  }

  @Override
  public WebElement findElementByCssSelector(String expr) {
    init();
    return Element.create(context).findElementByCssSelector(expr);
  }

  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    init();
    return Element.create(context).findElementsByCssSelector(expr);
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    init();
    return Element.create(context).findElementByTagName(tagName);
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    init();
    return Element.create(context).findElementsByTagName(tagName);
  }

  @Override
  public Object executeAsyncScript(String script, Object... args) {
    init();
    return Element.create(context).executeAsyncScript(script, args);
  }

  @Override
  public Object executeScript(String script, Object... args) {
    init();
    return Element.create(context).executeScript(script, args);
  }

  @Override
  public Keyboard getKeyboard() {
    init();
    return context.item().keyboard.get();
  }

  @Override
  public Mouse getMouse() {
    init();
    return context.item().mouse.get();
  }

  @Override
  public Capabilities getCapabilities() {
    init();
    return context.item().capabilities.get();
  }

  @Override
  public void close() {
    init();
    context.removeItem();
  }

  @Override
  public String getWindowHandle() {
    init();
    return context.itemId();
  }

  @Override
  public Set<String> getWindowHandles() {
    init();
    return context.itemIds();
  }

  @Override
  public Options manage() {
    init();
    return context.item().options.get();
  }

  @Override
  public Navigation navigate() {
    init();
    return context.item().navigation.get();
  }

  @Override
  public void quit() {
    init();
    context.removeItems();
    SettingsManager.deregister(settings);
    settings.get().cookieManager().getCookieStore().removeAll();
  }

  @Override
  public TargetLocator switchTo() {
    init();
    return context.item().targetLocator.get();
  }

  @Override
  public void kill() {
    quit();
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    init();
    JavaFxObject image = Util.exec(Pause.NONE, context.item().statusCode, new Sync<JavaFxObject>() {
      public JavaFxObject perform() {
        return JavaFx.getStatic(
            SwingFXUtils.class, Long.parseLong(context.item().engine.get().call("getUserAgent").toString())).
            call("fromFXImage", context.item().view.get().
                call("snapshot", JavaFx.getNew(SnapshotParameters.class, context.item().settingsId.get()),
                    JavaFx.getNew(WritableImage.class, context.item().settingsId.get(),
                        (int) Math.rint((Double) context.item().view.get().call("getWidth").unwrap()),
                        (int) Math.rint((Double) context.item().view.get().call("getHeight").unwrap()))), null);
      }
    }, context.item().settingsId.get());
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      JavaFx.getStatic(ImageIO.class, context.item().settingsId.get()).call("write", image, "png", out);
      return outputType.convertFromPngBytes(out.toByteArray());
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    } finally {
      Util.close(out);
    }
  }

  public static void main(String[] args) {}
}
