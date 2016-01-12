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
import java.io.InputStream;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
 * A Selenium-compatible and WebKit-based web driver written in pure Java.
 * <p>
 * See <a href="https://github.com/machinepublishers/jbrowserdriver#usage">
 * https://github.com/machinepublishers/jbrowserdriver#usage</a> for basic usage info.
 * <p>
 * Licensed under the Apache License version 2.0.
 * <p>
 * Sales and support: ops@machinepublishers.com
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
      String[] items = System.getProperty("java.class.path").split(File.pathSeparator);
      List<String> childJars = new ArrayList<String>();
      File tmpDir = Files.createTempDirectory("jbd").toFile();
      tmpDir.deleteOnExit();
      Random rand = new SecureRandom();
      for (int i = 0; i < items.length; i++) {
        if (items[i].endsWith(".jar")) {
          try (ZipFile jar = new ZipFile(items[i])) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
              ZipEntry entry = entries.nextElement();
              if (entry.getName().endsWith(".jar")) {
                try (InputStream in = jar.getInputStream(entry)) {
                  File childJar = new File(tmpDir,
                      Long.toString(Math.abs(rand.nextLong()), Character.MAX_RADIX) + ".jar");
                  Files.copy(in, childJar.toPath());
                  childJars.add(childJar.getCanonicalPath());
                  childJar.deleteOnExit();
                }
              }
            }
          }
        }
      }
      StringBuilder classpath = new StringBuilder();
      classpath.append(System.getProperty("java.class.path"));
      for (String childJar : childJars) {
        classpath.append(File.pathSeparator);
        classpath.append(childJar);
      }
      argsTmp.add(classpath.toString());
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
