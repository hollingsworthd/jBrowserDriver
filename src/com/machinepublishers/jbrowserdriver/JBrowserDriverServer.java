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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
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

import com.machinepublishers.jbrowserdriver.AppThread.Pause;
import com.machinepublishers.jbrowserdriver.AppThread.Sync;
import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;
import com.sun.webkit.network.CookieManager;

class JBrowserDriverServer extends RemoteObject implements JBrowserDriverRemote,
    WebDriver, JavascriptExecutor, FindsById, FindsByClassName, FindsByLinkText, FindsByName,
    FindsByCssSelector, FindsByTagName, FindsByXPath, HasInputDevices, HasCapabilities,
    TakesScreenshot, Killable {
  private static final AtomicInteger parentPort = new AtomicInteger();
  private static final AtomicInteger childPort = new AtomicInteger();
  private static final AtomicReference<SocketFactory> socketFactory = new AtomicReference<SocketFactory>();
  private static Registry registry;

  /*
   * RMI entry point.
   */
  public static void main(String[] args) {
    System.setProperty("sun.rmi.dgc.server.gcInterval", Long.toString(Long.MAX_VALUE));
    CookieManager.setDefault(new CookieStore());
    try {
      URL.setURLStreamHandlerFactory(new StreamHandler());
    } catch (Throwable t) {
      Field factory = null;
      try {
        factory = URL.class.getDeclaredField("factory");
        factory.setAccessible(true);
        Object curFac = factory.get(null);

        //assume we're in the Eclipse jar-in-jar loader
        Field chainedFactory = curFac.getClass().getDeclaredField("chainFac");
        chainedFactory.setAccessible(true);
        chainedFactory.set(curFac, new StreamHandler());
      } catch (Throwable t2) {
        try {
          //this should work regardless
          factory.set(null, new StreamHandler());
        } catch (Throwable t3) {
          Util.handleException(t3);
        }
      }
    }
    final String host = System.getProperty("java.rmi.server.hostname");
    parentPort.set(Integer.parseInt(args[0]));
    childPort.set(Integer.parseInt(args[1]));
    Registry registryTmp = null;
    final int maxTries = 5;
    for (int i = 1; i <= maxTries; i++) {
      try {
        if (childPort.get() <= 0) {
          childPort.set(findPort(host));
        }
        if (parentPort.get() <= 0) {
          parentPort.set(findPort(host));
        }
        socketFactory.set(new SocketFactory(host, parentPort.get(), childPort.get(), new SocketLock()));
        registryTmp = LocateRegistry.createRegistry(childPort.get(), socketFactory.get(), socketFactory.get());
        registryTmp.rebind("JBrowserDriverRemote", new JBrowserDriverServer());
        break;
      } catch (Throwable t) {
        if (i == maxTries) {
          Util.handleException(t);
        }
      }
    }

    registry = registryTmp;
    try {
      RMISocketFactory.setSocketFactory(socketFactory.get());
    } catch (IOException e) {
      Util.handleException(e);
    }
    System.out.println("parent on port " + parentPort.get());
    System.out.println("child on port " + childPort.get());
  }

  private static int findPort(String host) throws IOException {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket();
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress(host, 0));
      return socket.getLocalPort();
    } finally {
      Util.close(socket);
    }
  }

  static int childPort() {
    return childPort.get();
  }

  static SocketFactory socketFactory() {
    return socketFactory.get();
  }

  final AtomicReference<Context> context = new AtomicReference<Context>();

  public JBrowserDriverServer() throws RemoteException {}

  @Override
  public synchronized void setUp(final Settings settings) {
    SettingsManager.register(settings);
    context.set(new Context());
  }

  @Override
  public synchronized void storeCapabilities(final Capabilities capabilities) {
    context.get().capabilities.set(capabilities);
  }

  /**
   * Optionally call this method if you want JavaFX initialized and the browser
   * window opened immediately. Otherwise, initialization will happen lazily.
   */
  public synchronized void init() {
    context.get().init(this);
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   * 
   * @param settings
   *          New settings to take effect, superseding the original ones
   */
  public synchronized void reset(final Settings settings) {
    AppThread.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        context.get().item().engine.get().getLoadWorker().cancel();
        return null;
      }
    });
    Accessor.getPageFor(context.get().item().engine.get()).stop();
    ((CookieStore) CookieManager.getDefault()).clear();
    StatusMonitor.instance().clearStatusMonitor();
    LogsServer.instance().clear(null);
    SettingsManager.register(settings);
    context.get().reset(this);
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   */
  public synchronized void reset() {
    reset(SettingsManager.settings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized String getPageSource() {
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
  public synchronized String getCurrentUrl() {
    init();
    return AppThread.exec(Pause.NONE, context.get().statusCode, new Sync<String>() {
      public String perform() {
        return context.get().item().view.get().getEngine().getLocation();
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void pageWait() {
    context.get().item().httpListener.get().resetStatusCode();
    getStatusCode();
  }

  public synchronized int getStatusCode() {
    return getStatusCode(context.get().timeouts.get().getPageLoadTimeoutMS());
  }

  private synchronized int getStatusCode(long waitMS) {
    init();
    try {
      synchronized (context.get().statusCode) {
        if (context.get().statusCode.get() == 0) {
          context.get().statusCode.wait(waitMS);
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
  public synchronized String getTitle() {
    init();
    return AppThread.exec(Pause.NONE, context.get().statusCode, new Sync<String>() {
      public String perform() {
        return context.get().item().view.get().getEngine().getTitle();
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void get(final String url) {
    init();
    long start = System.currentTimeMillis();
    try {
      AppThread.exec(Pause.SHORT, context.get().statusCode,
          context.get().timeouts.get().getPageLoadTimeoutMS(), new Sync<Object>() {
            public Object perform() {
              context.get().item().engine.get().load(url);
              return null;
            }
          });
      long end = System.currentTimeMillis();
      if (context.get().timeouts.get().getPageLoadTimeoutMS() == 0) {
        getStatusCode();
      } else {
        long waitMS = context.get().timeouts.get().getPageLoadTimeoutMS() - (end - start);
        if (waitMS > 0) {
          getStatusCode(waitMS);
        }
      }
    } finally {
      if (context.get().statusCode.get() == 0) {
        AppThread.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
          @Override
          public Object perform() {
            context.get().item().engine.get().getLoadWorker().cancel();
            throw new TimeoutException(new StringBuilder()
                .append("Timeout of ")
                .append(context.get().timeouts.get().getPageLoadTimeoutMS())
                .append("ms reached.").toString());
          }
        });
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized ElementServer findElement(By by) {
    init();
    return ElementServer.create(context.get()).findElement(by);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElements(By by) {
    init();
    return ElementServer.create(context.get()).findElements(by);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized ElementServer findElementById(String id) {
    init();
    return ElementServer.create(context.get()).findElementById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElementsById(String id) {
    init();
    return ElementServer.create(context.get()).findElementsById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized ElementServer findElementByXPath(String expr) {
    init();
    return ElementServer.create(context.get()).findElementByXPath(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElementsByXPath(String expr) {
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
  public synchronized ElementServer findElementByPartialLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementByPartialLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElementsByLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementsByLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElementsByPartialLinkText(String text) {
    init();
    return ElementServer.create(context.get()).findElementsByPartialLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized ElementServer findElementByClassName(String cssClass) {
    init();
    return ElementServer.create(context.get()).findElementByClassName(cssClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElementsByClassName(String cssClass) {
    init();
    return ElementServer.create(context.get()).findElementsByClassName(cssClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized ElementServer findElementByName(String name) {
    init();
    return ElementServer.create(context.get()).findElementByName(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElementsByName(String name) {
    init();
    return ElementServer.create(context.get()).findElementsByName(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized ElementServer findElementByCssSelector(String expr) {
    init();
    return ElementServer.create(context.get()).findElementByCssSelector(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElementsByCssSelector(String expr) {
    init();
    return ElementServer.create(context.get()).findElementsByCssSelector(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized ElementServer findElementByTagName(String tagName) {
    init();
    return ElementServer.create(context.get()).findElementByTagName(tagName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized List findElementsByTagName(String tagName) {
    init();
    return ElementServer.create(context.get()).findElementsByTagName(tagName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Object executeAsyncScript(String script, Object... args) {
    init();
    return ElementServer.create(context.get()).executeAsyncScript(script, args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Object executeScript(String script, Object... args) {
    init();
    return ElementServer.create(context.get()).executeScript(script, args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized KeyboardServer getKeyboard() {
    init();
    return context.get().keyboard.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized MouseServer getMouse() {
    init();
    return context.get().mouse.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Capabilities getCapabilities() {
    init();
    return context.get().capabilities.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void close() {
    init();
    context.get().removeItem();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized String getWindowHandle() {
    init();
    return context.get().itemId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Set<String> getWindowHandles() {
    init();
    return context.get().itemIds();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized OptionsServer manage() {
    init();
    return context.get().options.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized LogsServer logs() {
    return LogsServer.instance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized NavigationServer navigate() {
    init();
    return context.get().navigation.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void quit() {
    getStatusCode();
    kill();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized TargetLocatorServer switchTo() {
    init();
    return context.get().targetLocator.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void kill() {
    final ContextItem item = context.get().item();
    if (item != null) {
      item.engine.get().getLoadWorker().cancel();
      Accessor.getPageFor(item.engine.get()).stop();
    }
    SettingsManager.register(null);
    StatusMonitor.instance().clearStatusMonitor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    return outputType.convertFromPngBytes(getScreenshot());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized byte[] getScreenshot() throws WebDriverException {
    init();
    return context.get().robot.get().screenshot();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized File cacheDir() {
    return StreamConnection.cacheDir();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized File attachmentsDir() {
    return StreamConnection.attachmentsDir();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized File mediaDir() {
    return StreamConnection.mediaDir();
  }
}