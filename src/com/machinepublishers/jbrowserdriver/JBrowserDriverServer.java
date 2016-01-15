/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC
 * 
 * Sales and support: ops@machinepublishers.com
 * Updates: https://github.com/MachinePublishers/jBrowserDriver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.machinepublishers.jbrowserdriver;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

class JBrowserDriverServer extends UnicastRemoteObject implements JBrowserDriverRemote,
    WebDriver, JavascriptExecutor, FindsById, FindsByClassName, FindsByLinkText, FindsByName,
    FindsByCssSelector, FindsByTagName, FindsByXPath, HasInputDevices, HasCapabilities,
    TakesScreenshot, Killable {

  private static Registry registry;

  /*
   * RMI entry point.
   */
  public static void main(String[] args) {
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() {
    init();
    return Util.exec(Pause.NONE, context.get().statusCode, new Sync<String>() {
      public String perform() {
        return context.get().item().view.get().getEngine().getTitle();
      }
    });
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElement(By by) {
    init();
    return ElementServer.create(context.get()).findElement(by);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElements(By by) {
    init();
    return ElementServer.create(context.get()).findElements(by);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementById(String id) {
    init();
    return ElementServer.create(context.get()).findElementById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsById(String id) {
    init();
    return ElementServer.create(context.get()).findElementsById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByXPath(String expr) {
    init();
    return ElementServer.create(context.get()).findElementByXPath(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByXPath(String expr) {
    init();
    return ElementServer.create(context.get()).findElementsByXPath(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByLinkText(final String text) {
    init();
    return ElementServer.create(context.get()).findElementByLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByPartialLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementByPartialLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementsByLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByPartialLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementsByPartialLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByClassName(String cssClass) {
    init();
    return ElementServer.create(context.get()).findElementByClassName(cssClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByClassName(String cssClass) {
    init();
    return ElementServer.create(context.get()).findElementsByClassName(cssClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByName(String name) {
    init();
    return ElementServer.create(context.get()).findElementByName(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByName(String name) {
    init();
    return ElementServer.create(context.get()).findElementsByName(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByCssSelector(String expr) {
    init();
    return ElementServer.create(context.get()).findElementByCssSelector(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByCssSelector(String expr) {
    init();
    return ElementServer.create(context.get()).findElementsByCssSelector(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByTagName(String tagName) {
    init();
    return ElementServer.create(context.get()).findElementByTagName(tagName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByTagName(String tagName) {
    init();
    return ElementServer.create(context.get()).findElementsByTagName(tagName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeAsyncScript(String script, Object... args) {
    init();
    return ElementServer.create(context.get()).executeAsyncScript(script, args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeScript(String script, Object... args) {
    init();
    return ElementServer.create(context.get()).executeScript(script, args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public KeyboardServer getKeyboard() {
    init();
    return context.get().keyboard.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MouseServer getMouse() {
    init();
    return context.get().mouse.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CapabilitiesServer getCapabilities() {
    init();
    return context.get().capabilities.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    init();
    context.get().removeItem();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWindowHandle() {
    init();
    return context.get().itemId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getWindowHandles() {
    init();
    return context.get().itemIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OptionsServer manage() {
    init();
    return context.get().options.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LogsServer logs() {
    return LogsServer.instance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NavigationServer navigate() {
    init();
    return context.get().item().navigation.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void quit() {
    final ContextItem item = context.get().item();
    if (item != null) {
      Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
        @Override
        public Object perform() {
          item.engine.get().getLoadWorker().cancel();
          return null;
        }
      });
      Accessor.getPageFor(item.engine.get()).stop();
    }
    SettingsManager.register(null);
    if (Settings.headless()) {
      Platform.exit();
    }
    StatusMonitor.instance().clearStatusMonitor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TargetLocatorServer switchTo() {
    init();
    return context.get().targetLocator.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void kill() {
    quit();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    return outputType.convertFromPngBytes(getScreenshot());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] getScreenshot() throws WebDriverException {
    init();
    return context.get().robot.get().screenshot();
  }
}