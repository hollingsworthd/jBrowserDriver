/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.HasInputDevices;
import org.openqa.selenium.internal.FindsByClassName;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.openqa.selenium.internal.Killable;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

class JBrowserDriverServer extends UnicastRemoteObject implements JBrowserDriverRemote,
    WebDriver, JavascriptExecutor, FindsById, FindsByClassName, FindsByLinkText, FindsByName,
    FindsByCssSelector, FindsByTagName, FindsByXPath, HasInputDevices, HasCapabilities,
    TakesScreenshot, Killable {

  private static Registry registry;

  /*
   * RMI entry point.
   */
  public static void main(String[] args) {
    Policy.init();
    final int port = Integer.parseInt(args[0]);
    Registry registryTmp = null;
    try {
      registryTmp = LocateRegistry.createRegistry(port);
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    }
    registry = registryTmp;

    try {
      registry.rebind("JBrowserDriverRemote", new JBrowserDriverServer());
      System.out.println("ready");
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    }
  }

  final AtomicReference<Context> context = new AtomicReference<Context>();

  public JBrowserDriverServer() throws RemoteException {}

  public void setUp(final Settings settings) {
    context.set(new Context(settings));
    SettingsManager.register(settings);
  }

  /**
   * Optionally call this method if you want JavaFX initialized and the browser
   * window opened immediately. Otherwise, initialization will happen lazily.
   */
  public void init() {
    context.get().init(this);
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   * 
   * @param settings
   *          New settings to take effect, superseding the original ones
   */
  public void reset(final Settings settings) {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        context.get().item().engine.get().getLoadWorker().cancel();
        return null;
      }
    });
    Accessor.getPageFor(context.get().item().engine.get()).stop();
    SettingsManager.settings().cookieStore().clear();
    StatusMonitor.instance().clearStatusMonitor();
    LogsServer.instance().clear();
    SettingsManager.register(settings);
    context.get().reset(this);
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   */
  public void reset() {
    reset(SettingsManager.settings());
  }

  @Override
  public String getPageSource() {
    init();
    WebElement element = ElementServer.create(context.get()).findElementByTagName("html");
    if (element != null) {
      String outerHtml = element.getAttribute("outerHTML");
      if (outerHtml != null && !outerHtml.isEmpty()) {
        return outerHtml;
      }
    }
    WebPage page = Accessor.getPageFor(context.get().item().engine.get());
    String html = page.getHtml(page.getMainFrame());
    if (html != null && !html.isEmpty()) {
      return html;
    }
    return page.getInnerText(page.getMainFrame());
  }

  @Override
  public String getCurrentUrl() {
    init();
    return Util.exec(Pause.NONE, context.get().statusCode, new Sync<String>() {
      public String perform() {
        return context.get().item().view.get().getEngine().getLocation();
      }
    });
  }

  public int getStatusCode() {
    init();
    try {
      synchronized (context.get().statusCode) {
        if (context.get().statusCode.get() == 0) {
          context.get().statusCode.wait(context.get().timeouts.get().getPageLoadTimeoutMS());
        }
      }
    } catch (InterruptedException e) {
      LogsServer.instance().exception(e);
    }
    return context.get().statusCode.get();
  }

  @Override
  public String getTitle() {
    init();
    return Util.exec(Pause.NONE, context.get().statusCode, new Sync<String>() {
      public String perform() {
        return context.get().item().view.get().getEngine().getTitle();
      }
    });
  }

  @Override
  public void get(final String url) {
    init();
    Util.exec(Pause.SHORT, context.get().statusCode, new Sync<Object>() {
      public Object perform() {
        context.get().item().engine.get().load(url);
        return null;
      }
    });
    try {
      synchronized (context.get().statusCode) {
        if (context.get().statusCode.get() == 0) {
          context.get().statusCode.wait(context.get().timeouts.get().getPageLoadTimeoutMS());
        }
      }
    } catch (InterruptedException e) {
      LogsServer.instance().exception(e);
    }
    if (context.get().statusCode.get() == 0) {
      Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
        @Override
        public Object perform() {
          context.get().item().engine.get().getLoadWorker().cancel();
          return null;
        }
      });
    }
  }

  @Override
  public ElementServer findElement(By by) {
    init();
    //TODO FIXME
    return null;//by.findElement(this);
  }

  @Override
  public List findElements(By by) {
    init();
    //TODO FIXME
    return null;//by.findElements(this);
  }

  @Override
  public ElementServer findElementById(String id) {
    init();
    return ElementServer.create(context.get()).findElementById(id);
  }

  @Override
  public List findElementsById(String id) {
    init();
    return ElementServer.create(context.get()).findElementsById(id);
  }

  @Override
  public ElementServer findElementByXPath(String expr) {
    init();
    return ElementServer.create(context.get()).findElementByXPath(expr);
  }

  @Override
  public List findElementsByXPath(String expr) {
    init();
    return ElementServer.create(context.get()).findElementsByXPath(expr);
  }

  @Override
  public ElementServer findElementByLinkText(final String text) {
    init();
    return ElementServer.create(context.get()).findElementByLinkText(text);
  }

  @Override
  public ElementServer findElementByPartialLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementByPartialLinkText(text);
  }

  @Override
  public List findElementsByLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementsByLinkText(text);
  }

  @Override
  public List findElementsByPartialLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementsByPartialLinkText(text);
  }

  @Override
  public ElementServer findElementByClassName(String cssClass) {
    init();
    return ElementServer.create(context.get()).findElementByClassName(cssClass);
  }

  @Override
  public List findElementsByClassName(String cssClass) {
    init();
    return ElementServer.create(context.get()).findElementsByClassName(cssClass);
  }

  @Override
  public ElementServer findElementByName(String name) {
    init();
    return ElementServer.create(context.get()).findElementByName(name);
  }

  @Override
  public List findElementsByName(String name) {
    init();
    return ElementServer.create(context.get()).findElementsByName(name);
  }

  @Override
  public ElementServer findElementByCssSelector(String expr) {
    init();
    return ElementServer.create(context.get()).findElementByCssSelector(expr);
  }

  @Override
  public List findElementsByCssSelector(String expr) {
    init();
    return ElementServer.create(context.get()).findElementsByCssSelector(expr);
  }

  @Override
  public ElementServer findElementByTagName(String tagName) {
    init();
    return ElementServer.create(context.get()).findElementByTagName(tagName);
  }

  @Override
  public List findElementsByTagName(String tagName) {
    init();
    return ElementServer.create(context.get()).findElementsByTagName(tagName);
  }

  @Override
  public Object executeAsyncScript(String script, Object... args) {
    init();
    return ElementServer.create(context.get()).executeAsyncScript(script, args);
  }

  @Override
  public Object executeScript(String script, Object... args) {
    init();
    return ElementServer.create(context.get()).executeScript(script, args);
  }

  @Override
  public KeyboardServer getKeyboard() {
    init();
    return context.get().keyboard.get();
  }

  @Override
  public MouseServer getMouse() {
    init();
    return context.get().mouse.get();
  }

  @Override
  public CapabilitiesServer getCapabilities() {
    init();
    return context.get().capabilities.get();
  }

  @Override
  public void close() {
    init();
    context.get().removeItem();
  }

  @Override
  public String getWindowHandle() {
    init();
    return context.get().itemId();
  }

  @Override
  public Set<String> getWindowHandles() {
    init();
    return context.get().itemIds();
  }

  @Override
  public OptionsServer manage() {
    init();
    return context.get().options.get();
  }

  @Override
  public LogsServer logs() {
    return LogsServer.instance();
  }

  @Override
  public NavigationServer navigate() {
    init();
    return context.get().item().navigation.get();
  }

  @Override
  public void quit() {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        context.get().item().engine.get().getLoadWorker().cancel();
        return null;
      }
    });
    Accessor.getPageFor(context.get().item().engine.get()).stop();
    SettingsManager.settings().cookieStore().clear();
    SettingsManager.register(null);
    if (Settings.headless()) {
      Platform.exit();
    }
    StatusMonitor.instance().clearStatusMonitor();
  }

  @Override
  public TargetLocatorServer switchTo() {
    init();
    return context.get().targetLocator.get();
  }

  @Override
  public void kill() {
    quit();
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    return outputType.convertFromPngBytes(getScreenshot());
  }

  @Override
  public byte[] getScreenshot() throws WebDriverException {
    init();
    BufferedImage image = Util.exec(Pause.NONE, context.get().statusCode, new Sync<BufferedImage>() {
      public BufferedImage perform() {
        return SwingFXUtils.fromFXImage(
            context.get().item().view.get().snapshot(
                new SnapshotParameters(),
                new WritableImage(
                    (int) Math.rint((Double) context.get().item().view.get().getWidth()),
                    (int) Math.rint((Double) context.get().item().view.get().getHeight()))),
            null);
      }
    });
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
      return null;
    } finally {
      Util.close(out);
    }
  }
}