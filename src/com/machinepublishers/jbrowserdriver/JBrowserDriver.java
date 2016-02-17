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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
    String property = System.getProperty("jbd.ports", "10000-10007"); //TODO handle arbitrary port on instantiation
    String[] ranges = property.split(",");
    for (int i = 0; i < ranges.length; i++) {
      String[] bounds = ranges[i].split("-");
      int low = Integer.parseInt(bounds[0]);
      int high = bounds.length > 1 ? Integer.parseInt(bounds[1]) : low;
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
        if (key != null && key.startsWith("jbd.rmi.")) {
          argsTmp.add("-D" + key.substring("jbd.rmi.".length()) + "=" + System.getProperty(key));
        } else if (key != null && key.startsWith("jbd.")) {
          argsTmp.add("-D" + key + "=" + System.getProperty(key));
        }
      }

      URL[] items = ((URLClassLoader) JBrowserDriver.class.getClassLoader()).getURLs();
      final File classpathDir = Files.createTempDirectory("jbd_classpath_").toFile();
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        @Override
        public void run() {
          FileUtils.deleteQuietly(classpathDir);
        }
      }));
      Random rand = new SecureRandom();
      List<String> paths = new ArrayList<String>();
      for (int i = 0; i < items.length; i++) {
        File curItem = new File(items[i].getPath());
        paths.add(curItem.getAbsoluteFile().toURI().toURL().toExternalForm());
        if (curItem.isFile() && items[i].getPath().endsWith(".jar")) {
          try (ZipFile jar = new ZipFile(items[i].getPath())) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
              ZipEntry entry = entries.nextElement();
              if (entry.getName().endsWith(".jar")) {
                try (InputStream in = jar.getInputStream(entry)) {
                  File childJar = new File(classpathDir,
                      Long.toString(Math.abs(rand.nextLong()), Math.min(36, Character.MAX_RADIX)) + ".jar");
                  Files.copy(in, childJar.toPath());
                  paths.add(childJar.getAbsoluteFile().toURI().toURL().toExternalForm());
                  childJar.deleteOnExit();
                }
              }
            }
          }
        }
      }
      Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
      manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, StringUtils.join(paths, ' '));
      File classpathJar = new File(classpathDir, "classpath.jar");
      classpathJar.deleteOnExit();
      new JarOutputStream(new FileOutputStream(classpathJar), manifest).close();
      argsTmp.add("-classpath");
      argsTmp.add(classpathJar.getCanonicalPath());
    } catch (Throwable t) {
      Logs.fatal(t);
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

  private final File tmpDir;

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
    File tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory("jbd_tmp_").toFile();
    } catch (Throwable t) {
      Logs.fatal(t);
    }
    this.tmpDir = tmpDir;
    final File thisTmpDir = this.tmpDir;
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        FileUtils.deleteQuietly(thisTmpDir);
      }
    }));

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
      Logs.fatal(t);
    }
    remote = instanceTmp;
    LogsRemote logsRemote = null;
    try {
      logsRemote = remote.logs();
    } catch (RemoteException e) {
      Logs.fatal(e);
    }
    logs = new Logs(logsRemote);
  }

  private void launchProcess(int port) {
    final AtomicBoolean ready = new AtomicBoolean();
    final String logPrefix = "[Port " + port + "] ";
    new Thread(new Runnable() {
      @Override
      public void run() {
        List<String> myArgs = new ArrayList<String>(args);
        myArgs.add("-Djava.io.tmpdir=" + tmpDir.getAbsolutePath());
        myArgs.add(JBrowserDriverServer.class.getName());
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
                    System.out.println(logPrefix + line);
                  }
                }
              } else {
                System.out.println(logPrefix + line);
              }
            }
          })
              .redirectError(new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              System.err.println(logPrefix + line);
            }
          })
              .destroyOnExit()
              .command(myArgs).execute();
        } catch (Throwable t) {
          Logs.fatal(t);
        }
        FileUtils.deleteQuietly(tmpDir);
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
    //TODO clear out tmp files except cache
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPageSource() {
    try {
      return remote.getPageSource();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() {
    try {
      return remote.getTitle();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void get(final String url) {
    try {
      remote.get(url);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElement(By by) {
    try {
      return Element.constructElement(remote.findElement(by), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElements(By by) {
    try {
      return Element.constructList(remote.findElements(by), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementById(String id) {
    try {
      return Element.constructElement(remote.findElementById(id), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsById(String id) {
    try {
      return Element.constructList(remote.findElementsById(id), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByXPath(String expr) {
    try {
      return Element.constructElement(remote.findElementByXPath(expr), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    try {
      return Element.constructList(remote.findElementsByXPath(expr), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByLinkText(final String text) {
    try {
      return Element.constructElement(remote.findElementByLinkText(text), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByPartialLinkText(String text) {
    try {
      return Element.constructElement(remote.findElementByPartialLinkText(text), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    try {
      return Element.constructList(remote.findElementsByLinkText(text), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    try {
      return Element.constructList(remote.findElementsByPartialLinkText(text), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByClassName(String cssClass) {
    try {
      return Element.constructElement(remote.findElementByClassName(cssClass), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    try {
      return Element.constructList(remote.findElementsByClassName(cssClass), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByName(String name) {
    try {
      return Element.constructElement(remote.findElementByName(name), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByName(String name) {
    try {
      return Element.constructList(remote.findElementsByName(name), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByCssSelector(String expr) {
    try {
      return Element.constructElement(remote.findElementByCssSelector(expr), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    try {
      return Element.constructList(remote.findElementsByCssSelector(expr), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByTagName(String tagName) {
    try {
      return Element.constructElement(remote.findElementByTagName(tagName), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    try {
      return Element.constructList(remote.findElementsByTagName(tagName), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeAsyncScript(String script, Object... args) {
    try {
      return Element.constructObject(remote.executeAsyncScript(script, args), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeScript(String script, Object... args) {
    try {
      return Element.constructObject(remote.executeScript(script, args), this, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWindowHandle() {
    try {
      return remote.getWindowHandle();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getWindowHandles() {
    try {
      return remote.getWindowHandles();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
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

  /**
   * @return Temporary directory where cached pages are saved.
   */
  public File cacheDir() {
    try {
      return remote.cacheDir();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * @return Temporary directory where downloaded files are saved.
   */
  public File attachmentsDir() {
    try {
      return remote.attachmentsDir();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  /**
   * @return Temporary directory where media files are saved.
   */
  public File mediaDir() {
    try {
      return remote.mediaDir();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }
}
