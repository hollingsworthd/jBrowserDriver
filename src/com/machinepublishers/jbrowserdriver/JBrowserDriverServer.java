/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2017 jBrowserDriver committers
 * https://github.com/MachinePublishers/jBrowserDriver
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
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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

import com.machinepublishers.jbrowserdriver.AppThread.Sync;
import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;
import com.sun.webkit.network.CookieManager;

class JBrowserDriverServer extends RemoteObject implements JBrowserDriverRemote,
    WebDriver, JavascriptExecutor, FindsById, FindsByClassName, FindsByLinkText, FindsByName,
    FindsByCssSelector, FindsByTagName, FindsByXPath, HasInputDevices, HasCapabilities,
    TakesScreenshot {
  private static final AtomicInteger childPort = new AtomicInteger();
  private static final AtomicReference<SocketFactory> socketFactory = new AtomicReference<SocketFactory>();
  private static Registry registry;

  /*
   * RMI entry point.
   */
  public static void main(String[] args) {
    try {
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
          //this should work regardless
          factory.set(null, new StreamHandler());
        }
      }
      final String host = System.getProperty("java.rmi.server.hostname");
      childPort.set((int) Long.parseLong(args[0]));
      long parentPort = Long.parseLong(args[1]);
      parentPort = parentPort < 0 ? 0 : parentPort;
      long parentAltPort = Long.parseLong(args[2]);
      parentAltPort = parentAltPort < 0 ? 0 : parentAltPort;
      Registry registryTmp = null;
      final int maxTries = 5;
      for (int i = 1; i <= maxTries; i++) {
        try {
          if (childPort.get() <= 0) {
            childPort.set(findPort(host));
          }
          socketFactory.set(new SocketFactory(host,
              new PortGroup(childPort.get(), parentPort, parentAltPort), new HashSet<SocketLock>()));

          registryTmp = LocateRegistry.createRegistry(childPort.get(), socketFactory.get(), socketFactory.get());
          break;
        } catch (Throwable t) {
          if (i == maxTries) {
            Util.handleException(t);
          }
        }
      }
      registry = registryTmp;
      registry.rebind("HeartbeatRemote", new HeartbeatServer());
      registry.rebind("JBrowserDriverRemote", new JBrowserDriverServer());

      RMISocketFactory.setSocketFactory(socketFactory.get());
      System.out.println("ready on ports " + childPort.get() + "/" + parentPort + "/" + parentAltPort);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
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
  public void setUp(final Settings settings) {
    SettingsManager.register(settings);
    context.set(new Context());
  }

  @Override
  public void storeCapabilities(final Capabilities capabilities) {
    context.get().capabilities.set(capabilities);
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
    AppThread.exec(
        new Sync<Object>() {
          @Override
          public Object perform() {
            context.get().item().engine.get().getLoadWorker().cancel();
            return null;
          }
        });
    Accessor.getPageFor(context.get().item().engine.get()).stop();
    ((CookieStore) CookieManager.getDefault()).clear();
    StatusMonitor.instance().clear();
    LogsServer.instance().clear(null);
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
    WebElement element = ElementServer.create(context.get().item()).findElementByTagName("html");
    if (element != null) {
      String outerHtml = element.getAttribute("outerHTML");
      if (outerHtml != null && !outerHtml.isEmpty()) {
        return outerHtml;
      }
    }
    return AppThread.exec(context.get().item().statusCode, new Sync<String>() {
      public String perform() {
        WebPage page = Accessor.getPageFor(context.get().item().engine.get());
        String html = page.getHtml(page.getMainFrame());
        if (html != null && !html.isEmpty()) {
          return html;
        }
        try {
          StringWriter stringWriter = new StringWriter();
          Transformer transformer = TransformerFactory.newInstance().newTransformer();
          transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
          transformer.transform(new DOMSource(context.get().item().engine.get().getDocument()), new StreamResult(stringWriter));
          return stringWriter.toString();
        } catch (Throwable t) {}
        return page.getInnerText(page.getMainFrame());
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCurrentUrl() {
    init();
    return AppThread.exec(context.get().item().statusCode, new Sync<String>() {
      public String perform() {
        return context.get().item().view.get().getEngine().getLocation();
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void pageWait() {
    context.get().item().httpListener.get().resetStatusCode();
    getStatusCode();
  }

  public int getStatusCode() {
    return getStatusCode(context.get().timeouts.get().getPageLoadTimeoutMS());
  }

  private int getStatusCode(long waitMS) {
    init();
    int statusCode;
    synchronized (context.get().item().statusCode) {
      long start = System.currentTimeMillis();
      while ((statusCode = context.get().item().statusCode.get()) <= 0) {
        try {
          long nextWait = waitMS - (System.currentTimeMillis() - start);
          if (nextWait >= 0) {
            context.get().item().statusCode.wait(nextWait);
          } else if (waitMS == 0) {
            context.get().item().statusCode.wait();
          } else {
            break;
          }
        } catch (InterruptedException e) {}
      }
    }
    return statusCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() {
    init();
    return AppThread.exec(context.get().item().statusCode, new Sync<String>() {
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
    long start = System.currentTimeMillis();
    try {
      AppThread.exec(context.get().item().statusCode,
          context.get().timeouts.get().getPageLoadTimeoutMS(), new Sync<Object>() {
            public Object perform() {
              context.get().item().httpListener.get().resetStatusCode();
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
      if (context.get().item().statusCode.get() == 0) {
        AppThread.exec(
            new Sync<Object>() {
              @Override
              public Object perform() {
                context.get().item().engine.get().getLoadWorker().cancel();
                throw new TimeoutException(new StringBuilder()
                    .append("Timeout of ")
                    .append(context.get().timeouts.get().getPageLoadTimeoutMS())
                    .append("ms reached.").toString());
              }
            }, context.get().timeouts.get().getPageLoadTimeoutMS());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElement(By by) {
    init();
    return ElementServer.create(context.get().item()).findElement(by);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElements(By by) {
    init();
    return ElementServer.create(context.get().item()).findElements(by);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementById(String id) {
    init();
    return ElementServer.create(context.get().item()).findElementById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsById(String id) {
    init();
    return ElementServer.create(context.get().item()).findElementsById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByXPath(String expr) {
    init();
    return ElementServer.create(context.get().item()).findElementByXPath(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByXPath(String expr) {
    init();
    return ElementServer.create(context.get().item()).findElementsByXPath(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByLinkText(final String text) {
    init();
    return ElementServer.create(context.get().item()).findElementByLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByPartialLinkText(String text) {
    init();
    return ElementServer.create(context.get().item()).findElementByPartialLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByLinkText(String text) {
    init();
    return ElementServer.create(context.get().item()).findElementsByLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByPartialLinkText(String text) {
    init();
    return ElementServer.create(context.get().item()).findElementsByPartialLinkText(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByClassName(String cssClass) {
    init();
    return ElementServer.create(context.get().item()).findElementByClassName(cssClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByClassName(String cssClass) {
    init();
    return ElementServer.create(context.get().item()).findElementsByClassName(cssClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByName(String name) {
    init();
    return ElementServer.create(context.get().item()).findElementByName(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByName(String name) {
    init();
    return ElementServer.create(context.get().item()).findElementsByName(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByCssSelector(String expr) {
    init();
    return ElementServer.create(context.get().item()).findElementByCssSelector(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByCssSelector(String expr) {
    init();
    return ElementServer.create(context.get().item()).findElementsByCssSelector(expr);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByTagName(String tagName) {
    init();
    return ElementServer.create(context.get().item()).findElementByTagName(tagName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByTagName(String tagName) {
    init();
    return ElementServer.create(context.get().item()).findElementsByTagName(tagName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeAsyncScript(String script, Object... args) {
    init();
    return ElementServer.create(context.get().item()).executeAsyncScript(script, args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeScript(String script, Object... args) {
    init();
    return ElementServer.create(context.get().item()).executeScript(script, args);
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
  public Capabilities getCapabilities() {
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
    return context.get().navigation.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void quit() {
    getStatusCode();
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
  @Deprecated
  public void kill() {}

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

  /**
   * {@inheritDoc}
   */
  @Override
  public File cacheDir() {
    return StreamConnection.cacheDir();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File attachmentsDir() {
    return StreamConnection.attachmentsDir();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File mediaDir() {
    return StreamConnection.mediaDir();
  }
}
