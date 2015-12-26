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

import java.io.File;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openqa.selenium.By;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
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
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import com.machinepublishers.jbrowserdriver.diagnostics.Test;

/**
 * Use this library like any other Selenium WebDriver or RemoteWebDriver (it implements Selenium's
 * JavascriptExecutor, HasInputDevices, TakesScreenshot, Killable, FindsById, FindsByClassName,
 * FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName, and FindsByXPath).
 * <p>
 * You can optionally pass a {@link Settings} object to the {@link JBrowserDriver#JBrowserDriver(Settings)}
 * constructor to specify a proxy, request headers, time zone, user agent, or navigator details.
 * By default, the browser mimics the fingerprint of Tor Browser.
 * <p>
 * Also, you can run as many instances of JBrowserDriver as you want (it's thread safe), and the
 * browser sessions will be fully isolated from each other when run in headless mode, which is the
 * default (when it's run with the GUI shown, some of the memory between instances is shared out of
 * necessity).
 * <p>
 * Example:
 * 
 * <pre>
 * import org.openqa.selenium.WebDriver;
 * import com.machinepublishers.jbrowserdriver.Timezone;
 * import com.machinepublishers.jbrowserdriver.JBrowserDriver;
 * import com.machinepublishers.jbrowserdriver.Settings;
 * 
 * public class Example {
 *   public static void main(String[] args) {
 * 
 *     // You can optionally pass a Settings object here,
 *     // constructed using Settings.Builder
 *     WebDriver driver = new JBrowserDriver(Settings.builder().timezone(Timezone.AMERICA_NEWYORK).build());
 * 
 *     // This will block for the page load and any
 *     // associated AJAX requests
 *     driver.get("http://example.com");
 * 
 *     // You can get status code unlike other Selenium drivers.
 *     // It blocks for AJAX requests and page loads after clicks
 *     // and keyboard events.
 *     System.out.println(((JBrowserDriver) driver).getStatusCode());
 * 
 *     // Returns the page source in its current state, including
 *     // any DOM updates that occurred after page load
 *     System.out.println(driver.getPageSource());
 *   }
 * }
 * </pre>
 * 
 * <b>Java System Properties:</b>
 * <p>
 * <b>jbd.traceconsole</b> Mirror trace-level log messages to standard out.
 * Otherwise these logs are only available through the Selenium APIs.Defaults to <b>false</b>.
 * <p>
 * <b>jbd.warnconsole</b> Mirror warning-level log messages to standard error. Otherwise
 * these logs are only available through the Selenium APIs. Defaults to <b>true</b>.
 * <p>
 * <b>jbd.maxlogs</b> Maximum number of log entries to store in memory, accessible via the
 * Selenium APIs. Oldest log entry is dropped once max is reached. Regardless of this
 * setting, logs are cleared per instance of JBrowserDriver after a call to quit(), reset(),
 * or Logs.get(String). Defaults to <b>5000</b>.
 * <p>
 * <b>jbd.browsergui</b> Show the browser GUI window. Defaults to <b>false</b>.
 * <p>
 * <b>jbd.quickrender</b> Exclude web page images and binary data from rendering. These
 * resources are still requested and can optionally be saved to disk (see the Settings
 * options). Some versions of Java are inefficient (memory-wise) in rendering images.
 * Defaults to <b>true</b>.
 * <p>
 * <b>jbd.blockads</b> Whether requests to ad/spam servers should be blocked. Based on hosts
 * in ad-hosts.txt in the source tree. Defaults to <b>true</b>.
 * <p>
 * <b>jbd.ajaxwait</b> The idle time (no pending AJAX requests) required in milliseconds
 * before a page is considered to have been loaded completely. For very slow or overloaded
 * CPUs, set a higher value. Defaults to <b>120</b>.
 * <p>
 * <b>jbd.ajaxresourcetimeout</b> The time in milliseconds after which an AJAX request will
 * be ignored when considering whether all AJAX requests have completed. Defaults to <b>2000</b>.
 * <p>
 * <b>jbd.pemfile</b> Specifies a source of trusted certificate authorities. Can take one of
 * four values: (1) <b>compatible</b> to accept standard browser certs, (2) <b>trustanything</b>
 * to accept any SSL cert, (3) a file path, or (4) a URL. The default when this property is not
 * set is your JRE's keystore, so you can use JDK's keytool to import specific certs.
 * <p>
 * <b>jbd.maxrouteconnections</b> Maximum number of concurrent connections to a specific
 * host+proxy combo. Defaults to <b>8</b>.
 * <p>
 * <b>jbd.maxconnections</b> Maximum number of concurrent connections overall.
 * Defaults to <b>3000</b>.
 * <p>
 */
public class JBrowserDriver implements WebDriver, JavascriptExecutor, FindsById,
    FindsByClassName, FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName,
    FindsByXPath, HasInputDevices, HasCapabilities, TakesScreenshot, Killable {

  static {
    if (System.getSecurityManager() == null) {
      try {
        File policy = File.createTempFile("jbd", ".policy");
        policy.deleteOnExit();
        Files.write(policy.toPath(), "grant{permission java.security.AllPermission;};".getBytes("utf-8"));
        System.setProperty("java.security.policy", policy.getAbsolutePath());
        System.setSecurityManager(new SecurityManager());
      } catch (Throwable t) {
        //TODO
        t.printStackTrace();
      }
    }
  }

  private static final List<String> args;

  static {
    List<String> argsTmp = new ArrayList<String>();
    try {
      File javaBin = new File(System.getProperty("java.home") + "/bin/java");
      if (!javaBin.exists()) {
        //probably means we're on a MS Windows server 
        javaBin = new File(javaBin.getCanonicalPath() + ".exe");
      }
      argsTmp.add(javaBin.getCanonicalPath());
      for (Object keyObj : System.getProperties().keySet()) {
        String key = keyObj.toString();
        argsTmp.add("-D" + key + "=" + System.getProperty(key));
      }
      argsTmp.add("-classpath");
      argsTmp.add(System.getProperty("java.class.path"));
      argsTmp.add(JBrowserDriverServer.class.getName());
    } catch (Throwable t) {
      //TODO
      t.printStackTrace();
    }
    args = Collections.unmodifiableList(argsTmp);
  }

  /**
   * Use this string on sendKeys functions to delete text.
   */
  public static final String KEYBOARD_DELETE;

  static {
    final int CHARS_TO_DELETE = 60;
    StringBuilder builder = new StringBuilder();
    String key = Keys.BACK_SPACE.toString();
    for (int i = 0; i < CHARS_TO_DELETE; i++) {
      builder.append(key);
    }
    key = Keys.DELETE.toString();
    for (int i = 0; i < CHARS_TO_DELETE; i++) {
      builder.append(key);
    }
    KEYBOARD_DELETE = builder.toString();
  }

  private final JBrowserDriverRemote remote;

  /**
   * Run diagnostic tests.
   * 
   * @return Errors or an empty list if no errors found.
   */
  public static List<String> test() {
    return Test.run();
  }

  /**
   * Constructs a browser with default settings, UTC timezone, and no proxy.
   */
  public JBrowserDriver() {
    this(Settings.builder().build());
  }

  /**
   * Use Settings.Builder to create settings to pass to this constructor.
   * 
   * @param settings
   */
  public JBrowserDriver(final Settings settings) {
    launchProcess();
    JBrowserDriverRemote instanceTmp = null;
    try {
      instanceTmp = (JBrowserDriverRemote) LocateRegistry.getRegistry(9012).lookup("JBrowserDriverRemote");
      instanceTmp.setUp(settings);
    } catch (Throwable t) {
      Logs.logsFor(1l).exception(t);
    }
    remote = instanceTmp;
  }

  JBrowserDriver(JBrowserDriverRemote remote) {
    this.remote = remote;
  }

  private static void launchProcess() {
    final AtomicBoolean ready = new AtomicBoolean();
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          new ProcessExecutor()
              .environment(System.getenv())
              .redirectOutput(new LogOutputStream() {
            boolean done = false;

            @Override
            protected void processLine(String line) {
              if (!done) {
                synchronized (ready) {
                  if ("ready".equals(line)) {
                    ready.set(true);
                    ready.notify();
                    done = true;
                  }
                }
              }
            }
          })
              .destroyOnExit()
              .command(args).execute();
        } catch (Throwable t) {
          //TODO
          t.printStackTrace();
        }
      }
    }).start();
    synchronized (ready) {
      while (!ready.get()) {
        try {
          ready.wait();
          break;
        } catch (InterruptedException e) {}
      }
    }
  }

  /**
   * Optionally call this method if you want JavaFX initialized and the browser
   * window opened immediately. Otherwise, initialization will happen lazily.
   */
  public void init() {
    try {
      remote.init();
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
    }
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   * 
   * @param settings
   *          New settings to take effect, superseding the original ones
   */
  public void reset(final Settings settings) {
    try {
      remote.reset(settings);
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
    }
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   */
  public void reset() {
    try {
      remote.reset();
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
    }
  }

  @Override
  public String getPageSource() {
    try {
      return remote.getPageSource();
    } catch (RemoteException e) {
      //TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public String getCurrentUrl() {
    try {
      return remote.getCurrentUrl();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  public int getStatusCode() {
    try {
      return remote.getStatusCode();
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
      return -1;
    }
  }

  @Override
  public String getTitle() {
    try {
      return remote.getTitle();
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void get(final String url) {
    try {
      remote.get(url);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public WebElement findElement(By by) {
    try {
      return new Element(remote.findElement(by));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElements(By by) {
    try {
      return Element.constructList(remote.findElements(by));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementById(String id) {
    try {
      return new Element(remote.findElementById(id));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    try {
      return Element.constructList(remote.findElementsById(id));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByXPath(String expr) {
    try {
      return new Element(remote.findElementByXPath(expr));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    try {
      return Element.constructList(remote.findElementsByXPath(expr));
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    try {
      return new Element(remote.findElementByLinkText(text));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    try {
      return new Element(remote.findElementByPartialLinkText(text));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    try {
      return Element.constructList(remote.findElementsByLinkText(text));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    try {
      return Element.constructList(remote.findElementsByPartialLinkText(text));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    try {
      return new Element(remote.findElementByClassName(cssClass));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    try {
      return Element.constructList(remote.findElementsByClassName(cssClass));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByName(String name) {
    try {
      return new Element(remote.findElementByName(name));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    try {
      return Element.constructList(remote.findElementsByName(name));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByCssSelector(String expr) {
    try {
      return new Element(remote.findElementByCssSelector(expr));
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    try {
      return Element.constructList(remote.findElementsByCssSelector(expr));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    try {
      return new Element(remote.findElementByTagName(tagName));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    try {
      return Element.constructList(remote.findElementsByTagName(tagName));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Object executeAsyncScript(String script, Object... args) {
    try {
      return Element.constructObject(remote.executeAsyncScript(script, args));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Object executeScript(String script, Object... args) {
    try {
      return Element.constructObject(remote.executeScript(script, args));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public org.openqa.selenium.interactions.Keyboard getKeyboard() {
    try {
      return new Keyboard(remote.getKeyboard());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public org.openqa.selenium.interactions.Mouse getMouse() {
    try {
      return new Mouse(remote.getMouse());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public org.openqa.selenium.Capabilities getCapabilities() {
    try {
      return new Capabilities(remote.getCapabilities());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void close() {
    try {
      remote.close();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public String getWindowHandle() {
    try {
      return remote.getWindowHandle();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Set<String> getWindowHandles() {
    try {
      return remote.getWindowHandles();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Options manage() {
    try {
      return new com.machinepublishers.jbrowserdriver.Options(remote.manage());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Navigation navigate() {
    try {
      return new com.machinepublishers.jbrowserdriver.Navigation(remote.navigate());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void quit() {
    try {
      remote.quit();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public TargetLocator switchTo() {
    try {
      return new com.machinepublishers.jbrowserdriver.TargetLocator(remote.switchTo());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void kill() {
    try {
      remote.kill();
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
    }
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    try {
      return outputType.convertFromPngBytes(remote.getScreenshot());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }
}
