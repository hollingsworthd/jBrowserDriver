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
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
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
import org.openqa.selenium.logging.LogEntries;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.listener.ProcessListener;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.process.PidProcess;
import org.zeroturnaround.process.Processes;

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

  //TODO handle jbd.fork=false

  /**
   * Use this string on sendKeys functions to delete text.
   */
  public static final String KEYBOARD_DELETE;
  private static final List<Integer> ports = new ArrayList<Integer>();
  private static final List<String> args;
  private static final List<Object> waiting = new ArrayList<Object>();
  private final Object key = new Object();
  private final JBrowserDriverRemote remote;
  private final Logs logs;
  private final AtomicReference<Process> process = new AtomicReference<Process>();
  private final int port;
  private final AtomicReference<OptionsLocal> options = new AtomicReference<OptionsLocal>();

  static {
    String property = System.getProperty("jbd.ports", "10000-10007");
    String[] ranges = property.split(",");
    for (int i = 0; i < ranges.length; i++) {
      String[] bounds = ranges[i].split("-");
      int low = Integer.parseInt(bounds[0]);
      int high = Integer.parseInt(bounds[1]);
      for (int j = low; j <= high; j++) {
        ports.add(j);
      }
    }
  }

  static {
    Policy.init();
  }

  static {
    List<String> argsTmp = new ArrayList<String>();
    try {
      File javaBin = new File(System.getProperty("java.home") + "/bin/java");
      if (!javaBin.exists()) {
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
      t.printStackTrace();
    }
    args = Collections.unmodifiableList(argsTmp);
  }

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
    synchronized (ports) {
      if (ports.isEmpty()) {
        waiting.add(key);
        while (true) {
          try {
            ports.wait();
            if (key.equals(waiting.get(0)) && !ports.isEmpty()) {
              break;
            }
          } catch (InterruptedException e) {}
        }
        waiting.remove(key);
      }
      port = ports.remove(0);
    }
    launchProcess(port);
    JBrowserDriverRemote instanceTmp = null;
    try {
      instanceTmp = (JBrowserDriverRemote) LocateRegistry.getRegistry(port).lookup("JBrowserDriverRemote");
      instanceTmp.setUp(settings);
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    }
    remote = instanceTmp;
    LogsRemote logsRemote = null;
    try {
      logsRemote = remote.logs();
    } catch (RemoteException e) {
      e.printStackTrace();
    }
    logs = new Logs(logsRemote);
  }

  private void launchProcess(int port) {
    final AtomicBoolean ready = new AtomicBoolean();
    new Thread(new Runnable() {
      @Override
      public void run() {
        List<String> myArgs = new ArrayList<String>(args);
        myArgs.add(Integer.toString(port));
        try {
          new ProcessExecutor()
              .environment(System.getenv())
              .addListener(new ProcessListener() {
            @Override
            public void afterStart(Process proc, ProcessExecutor executor) {
              process.set(proc);
            }
          })
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
                  } else {
                    System.out.println(line);
                  }
                }
              } else {
                System.out.println(line);
              }
            }
          })
              .redirectError(new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              System.err.println(line);
            }
          })
              .destroyOnExit()
              .command(myArgs).execute();
        } catch (Throwable t) {
          logs.exception(t);
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
      logs.exception(e);
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
      logs.exception(e);
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
      logs.exception(e);
    }
  }

  @Override
  public String getPageSource() {
    try {
      return remote.getPageSource();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public String getCurrentUrl() {
    try {
      return remote.getCurrentUrl();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  public int getStatusCode() {
    try {
      return remote.getStatusCode();
    } catch (RemoteException e) {
      logs.exception(e);
      return -1;
    }
  }

  @Override
  public String getTitle() {
    try {
      return remote.getTitle();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public void get(final String url) {
    try {
      remote.get(url);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public WebElement findElement(By by) {
    try {
      return Element.constructElement(remote.findElement(by), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElements(By by) {
    try {
      return Element.constructList(remote.findElements(by), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementById(String id) {
    try {
      return Element.constructElement(remote.findElementById(id), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    try {
      return Element.constructList(remote.findElementsById(id), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByXPath(String expr) {
    try {
      return Element.constructElement(remote.findElementByXPath(expr), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    try {
      return Element.constructList(remote.findElementsByXPath(expr), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    try {
      return Element.constructElement(remote.findElementByLinkText(text), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    try {
      return Element.constructElement(remote.findElementByPartialLinkText(text), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    try {
      return Element.constructList(remote.findElementsByLinkText(text), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    try {
      return Element.constructList(remote.findElementsByPartialLinkText(text), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    try {
      return Element.constructElement(remote.findElementByClassName(cssClass), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    try {
      return Element.constructList(remote.findElementsByClassName(cssClass), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByName(String name) {
    try {
      return Element.constructElement(remote.findElementByName(name), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    try {
      return Element.constructList(remote.findElementsByName(name), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByCssSelector(String expr) {
    try {
      return Element.constructElement(remote.findElementByCssSelector(expr), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    try {
      return Element.constructList(remote.findElementsByCssSelector(expr), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    try {
      return Element.constructElement(remote.findElementByTagName(tagName), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    try {
      return Element.constructList(remote.findElementsByTagName(tagName), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public Object executeAsyncScript(String script, Object... args) {
    try {
      return Element.constructObject(remote.executeAsyncScript(script, args), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Object executeScript(String script, Object... args) {
    try {
      return Element.constructObject(remote.executeScript(script, args), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public org.openqa.selenium.interactions.Keyboard getKeyboard() {
    try {
      KeyboardRemote keyboard = remote.getKeyboard();
      if (keyboard == null) {
        return null;
      }
      return new Keyboard(keyboard, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public org.openqa.selenium.interactions.Mouse getMouse() {
    try {
      MouseRemote mouse = remote.getMouse();
      if (mouse == null) {
        return null;
      }
      return new Mouse(mouse, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public org.openqa.selenium.Capabilities getCapabilities() {
    try {
      CapabilitiesRemote capabilities = remote.getCapabilities();
      if (capabilities == null) {
        return null;
      }
      return new Capabilities(capabilities, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public void close() {
    try {
      remote.close();
      Set<String> handles = getWindowHandles();
      if (handles == null || handles.isEmpty()) {
        quit();
      }
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public String getWindowHandle() {
    try {
      return remote.getWindowHandle();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Set<String> getWindowHandles() {
    try {
      return remote.getWindowHandles();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Options manage() {
    if (options.get() == null) {
      try {
        OptionsRemote optionsRemote = remote.manage();
        if (optionsRemote == null) {
          return null;
        }
        return new com.machinepublishers.jbrowserdriver.Options(optionsRemote, logs);
      } catch (RemoteException e) {
        logs.exception(e);
        return null;
      }
    } else {
      return options.get();
    }
  }

  @Override
  public Navigation navigate() {
    try {
      NavigationRemote navigation = remote.navigate();
      if (navigation == null) {
        return null;
      }
      return new com.machinepublishers.jbrowserdriver.Navigation(navigation, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  private void endProcess() {
    try {
      PidProcess pidProcess = Processes.newPidProcess(process.get());
      if (!pidProcess.destroyGracefully().waitFor(10, TimeUnit.SECONDS)) {
        pidProcess.destroyForcefully();
      }
    } catch (Throwable t) {
      process.get().destroyForcibly();
    }
    synchronized (ports) {
      ports.add(port);
      ports.notifyAll();
    }
  }

  private void saveData() {
    try {
      OptionsRemote optionsRemote = remote.manage();
      Set<Cookie> cookiesLocal = optionsRemote.getCookies();
      LogsRemote logsRemote = optionsRemote.logs();
      final LogEntries entries = logsRemote.getRemote(null).toLogEntries();
      final Set<String> types = logsRemote.getAvailableLogTypes();
      org.openqa.selenium.logging.Logs logsLocal = new org.openqa.selenium.logging.Logs() {
        @Override
        public Set<String> getAvailableLogTypes() {
          return types;
        }

        @Override
        public LogEntries get(String logType) {
          return entries;
        }
      };
      options.set(new OptionsLocal(cookiesLocal, logsLocal));
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void quit() {
    saveData();
    try {
      remote.quit();
    } catch (RemoteException e) {
      logs.exception(e);
    }
    endProcess();
  }

  @Override
  public TargetLocator switchTo() {
    try {
      TargetLocatorRemote locator = remote.switchTo();
      if (locator == null) {
        return null;
      }
      return new com.machinepublishers.jbrowserdriver.TargetLocator(locator, this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public void kill() {
    saveData();
    try {
      remote.kill();
    } catch (RemoteException e) {
      logs.exception(e);
    }
    endProcess();
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    try {
      byte[] bytes = remote.getScreenshot();
      if (bytes == null) {
        return null;
      }
      return outputType.convertFromPngBytes(bytes);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }
}
