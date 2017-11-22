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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.interactions.SourceType;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.ErrorHandler;
import org.openqa.selenium.remote.FileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.listener.ProcessListener;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.process.PidProcess;
import org.zeroturnaround.process.Processes;

import com.google.common.collect.ImmutableMap;
import com.machinepublishers.jbrowserdriver.diagnostics.Test;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

/**
 * A Selenium-compatible and WebKit-based web driver written in pure Java.
 * <p>
 * See <a href="https://github.com/machinepublishers/jbrowserdriver#usage">
 * https://github.com/machinepublishers/jbrowserdriver#usage</a> for basic usage info.
 * <p>
 * Licensed under the Apache License version 2.0.
 */
public class JBrowserDriver extends RemoteWebDriver {

  //TODO handle jbd.fork=false

  /**
   * This can be passed to sendKeys to delete all the text in a text field.
   * 
   * @deprecated send {@link org.openqa.selenium.Keys#CONTROL Ctrl}+a (not Ctrl+A) chord and then {@link org.openqa.selenium.Keys#BACK_SPACE BACK_SPACE}.
   */
  @Deprecated
  public static final String KEYBOARD_DELETE = Util.KEYBOARD_DELETE;
  private static final AtomicInteger runningInstances = new AtomicInteger(0);
  private static final Set<SocketLock> locks = new HashSet<SocketLock>();
  private static final Set<Job> waiting = new LinkedHashSet<Job>();
  private static final Set<PortGroup> portGroupsActive = new LinkedHashSet<PortGroup>();
  private static final String JAVA_BIN;
  private static final List<String> inheritedArgs;
  private static volatile List<String> classpathSimpleArgs;
  private static volatile List<String> classpathUnpackedArgs;
  private static final AtomicReference<List<String>> classpathArgs = new AtomicReference<>();
  private static final AtomicBoolean firstLaunch = new AtomicBoolean(true);
  private static final Set<String> filteredLogs = Collections.unmodifiableSet(
      new HashSet<String>(Arrays.asList(new String[] {
          "Warning: Single GUI Threadiong is enabled, FPS should be slower"
      })));
  private static final AtomicLong sessionIdCounter = new AtomicLong();

  static {
    List<String> inheritedArgsTmp = new ArrayList<String>();
    File javaBin = new File(System.getProperty("java.home") + "/bin/java");
    if (!javaBin.exists()) {
      javaBin = new File(javaBin.getAbsolutePath() + ".exe");
    }
    JAVA_BIN = javaBin.getAbsolutePath();
    try {
      for (Object keyObj : System.getProperties().keySet()) {
        String key = keyObj.toString();
        if (key != null && key.startsWith("jbd.rmi.")) {
          inheritedArgsTmp.add("-D" + key.substring("jbd.rmi.".length()) + "=" + System.getProperty(key));
        }
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    inheritedArgs = Collections.unmodifiableList(inheritedArgsTmp);

  }

  private static void initClasspath() {
    List<String> classpathSimpleTmp = new ArrayList<String>();
    List<String> classpathUnpackedTmp = new ArrayList<String>();
    try {
      List<File> classpathElements = new FastClasspathScanner().getUniqueClasspathElements();
      final File classpathDir = Files.createTempDirectory("jbd_classpath_").toFile();
      Runtime.getRuntime().addShutdownHook(new FileRemover(classpathDir));
      List<String> pathsSimple = new ArrayList<String>();
      List<String> pathsUnpacked = new ArrayList<String>();
      for (File curElement : classpathElements) {
        String rootLevelElement = curElement.getAbsoluteFile().toURI().toURL().toExternalForm();
        pathsSimple.add(rootLevelElement);
        pathsUnpacked.add(rootLevelElement);
        if (curElement.isFile() && curElement.getPath().endsWith(".jar")) {
          try (ZipFile jar = new ZipFile(curElement)) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
              ZipEntry entry = entries.nextElement();
              if (entry.getName().endsWith(".jar")) {
                try (InputStream in = jar.getInputStream(entry)) {
                  File childJar = new File(classpathDir,
                      Util.randomFileName() + ".jar");
                  Files.copy(in, childJar.toPath());
                  pathsUnpacked.add(childJar.getAbsoluteFile().toURI().toURL().toExternalForm());
                  childJar.deleteOnExit();
                }
              }
            }
          }
        }
      }
      classpathSimpleTmp = createClasspathJar(classpathDir, "classpath-simple.jar", pathsSimple);
      classpathUnpackedTmp = createClasspathJar(classpathDir, "classpath-unpacked.jar", pathsUnpacked);
    } catch (Throwable t) {
      Util.handleException(t);
    }
    classpathSimpleArgs = Collections.unmodifiableList(classpathSimpleTmp);
    classpathUnpackedArgs = Collections.unmodifiableList(classpathUnpackedTmp);
  }

  private static List<String> createClasspathJar(File dir, String jarName, List<String> manifestClasspath)
      throws IOException {
    List<String> classpathArgs = new ArrayList<String>();
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH,
        StringUtils.join(manifestClasspath, ' '));
    File classpathJar = new File(dir, jarName);
    classpathJar.deleteOnExit();
    try (JarOutputStream stream = new JarOutputStream(
        new FileOutputStream(classpathJar), manifest)) {}
    classpathArgs.add("-classpath");
    classpathArgs.add(classpathJar.getCanonicalPath());
    return classpathArgs;
  }

  public static void initWorkThread() {
    int previousInstanceCount = runningInstances.getAndIncrement();
    if (previousInstanceCount > 0) {
      return;
    }
    Thread work = new Thread(new Runnable() {
      @Override
      public void run() {
        synchronized (waiting) {
          while (runningInstances.get() > 0) {
            List<Job> selectedJobs = new ArrayList<Job>();
            for (Job job : waiting) {
              for (PortGroup curPortGroup : job.settings.portGroups()) {
                boolean conflicts = false;
                for (PortGroup curUsed : portGroupsActive) {
                  if (curUsed.conflicts(curPortGroup)) {
                    conflicts = true;
                    break;
                  }
                }
                if (!conflicts) {
                  job.portGroup.set(curPortGroup);
                  break;
                }
              }
              if (job.portGroup.get() != null) {
                selectedJobs.add(job);
                portGroupsActive.add(job.portGroup.get());
              }
            }
            for (Job job : selectedJobs) {
              waiting.remove(job);
              synchronized (job) {
                job.notifyAll();
              }
            }
            try {
              waiting.wait();
            } catch (InterruptedException e) {}
          }
        }
      }
    });
    work.setDaemon(true);
    work.setName("JBrowserDriver queued instance handler");
    work.start();
  }

  /**
   * Run diagnostic tests.
   * 
   * @return Errors or an empty list if no errors found.
   */
  public static List<String> test() {
    return Test.run();
  }

  private final JBrowserDriverRemote remote;
  private final Logs logs;
  private final AtomicReference<Process> process = new AtomicReference<Process>();
  private final AtomicBoolean processEnded = new AtomicBoolean();
  private final AtomicReference<PortGroup> configuredPortGroup = new AtomicReference<PortGroup>();
  private final AtomicReference<PortGroup> actualPortGroup = new AtomicReference<PortGroup>();
  private final AtomicReference<OptionsLocal> options = new AtomicReference<OptionsLocal>();
  private final SessionId sessionId;
  private final SocketLock lock = new SocketLock();
  private final File tmpDir;
  private final FileRemover shutdownHook;
  private final Thread heartbeatThread;

  /**
   * Constructs a browser with default settings, UTC timezone, and no proxy.
   */
  public JBrowserDriver() {
    this(Settings.builder().build());
  }

  /**
   * Use {@link Settings#builder()} ...buildCapabilities() to create settings to pass to this constructor.
   * 
   * This constructor is mostly useful for Selenium Server itself to use.
   * 
   * @param capabilities
   */
  public JBrowserDriver(Capabilities capabilities) {
    this(Settings.builder().build(capabilities));
    Map capabilitiesMap = new HashMap(capabilities.asMap());
    capabilitiesMap.remove("proxy");
    try {
      synchronized (lock.validated()) {
        remote.storeCapabilities(new MutableCapabilities(capabilitiesMap));
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Use {@link Settings#builder()} ...build() to create settings to pass to this constructor.
   * 
   * @param settings
   */
  public JBrowserDriver(final Settings settings) {
    initWorkThread();
    synchronized (locks) {
      locks.add(lock);
    }
    File tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory("jbd_tmp_").toFile();
    } catch (Throwable t) {
      Util.handleException(t);
    }
    this.tmpDir = tmpDir;
    this.shutdownHook = new FileRemover(tmpDir);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    final Job job = new Job(settings, configuredPortGroup);
    synchronized (waiting) {
      waiting.add(job);
      waiting.notifyAll();
    }
    synchronized (job) {
      while (configuredPortGroup.get() == null) {
        try {
          job.wait();
        } catch (InterruptedException e) {}
      }
    }
    SessionId sessionIdTmp = null;
    if (!settings.customClasspath()) {
      synchronized (firstLaunch) {
        if (firstLaunch.compareAndSet(true, false)) {
          initClasspath();
          classpathArgs.set(classpathUnpackedArgs);
          sessionIdTmp = new SessionId(launchProcess(settings, configuredPortGroup.get()));
          if (actualPortGroup.get() == null) {
            classpathArgs.set(classpathSimpleArgs);
          }
        }
      }
    }
    if (actualPortGroup.get() == null) {
      sessionIdTmp = new SessionId(launchProcess(settings, configuredPortGroup.get()));
    }
    sessionId = sessionIdTmp;
    if (actualPortGroup.get() == null) {
      endProcess();
      Util.handleException(new IllegalStateException("Could not launch browser."));
    }
    HeartbeatRemote heartbeatTmp = null;
    JBrowserDriverRemote instanceTmp = null;
    try {
      synchronized (lock.validated()) {
        Registry registry = LocateRegistry
            .getRegistry(settings.host(), (int) actualPortGroup.get().child,
                new SocketFactory(settings.host(), actualPortGroup.get(), locks));
        heartbeatTmp = (HeartbeatRemote) registry.lookup("HeartbeatRemote");
        instanceTmp = (JBrowserDriverRemote) registry.lookup("JBrowserDriverRemote");
        instanceTmp.setUp(settings);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    final HeartbeatRemote heartbeat = heartbeatTmp;
    heartbeatThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          if (processEnded.get()) {
            return;
          }
          try {
            heartbeat.heartbeat();
          } catch (RemoteException e) {}
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {}
        }
      }
    });
    heartbeatThread.setName("Heartbeat");
    heartbeatThread.start();
    remote = instanceTmp;
    LogsRemote logsRemote = null;
    try {
      synchronized (lock.validated()) {
        logsRemote = remote.logs();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    logs = new Logs(logsRemote, lock);
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      super.finalize();
    } catch (Throwable t) {}
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      shutdownHook.run();
    } catch (Throwable t) {}
  }

  private String launchProcess(final Settings settings, final PortGroup portGroup) {
    final AtomicBoolean ready = new AtomicBoolean();
    final AtomicReference<String> logPrefix = new AtomicReference<String>("");
    new Thread(new Runnable() {
      @Override
      public void run() {
        List<String> myArgs = new ArrayList<String>();
        myArgs.add(settings.javaBinary() == null ? JAVA_BIN : settings.javaBinary());
        myArgs.addAll(inheritedArgs);
        if (!settings.customClasspath()) {
          myArgs.addAll(classpathArgs.get());
        }
        if (settings.javaExportModules()) {
          myArgs.add("-XaddExports:javafx.web/com.sun.webkit.network=ALL-UNNAMED");
          myArgs.add("-XaddExports:javafx.web/com.sun.webkit.network.about=ALL-UNNAMED");
          myArgs.add("-XaddExports:javafx.web/com.sun.webkit.network.data=ALL-UNNAMED");
          myArgs.add("-XaddExports:java.base/sun.net.www.protocol.http=ALL-UNNAMED");
          myArgs.add("-XaddExports:java.base/sun.net.www.protocol.https=ALL-UNNAMED");
          myArgs.add("-XaddExports:java.base/sun.net.www.protocol.file=ALL-UNNAMED");
          myArgs.add("-XaddExports:java.base/sun.net.www.protocol.ftp=ALL-UNNAMED");
          myArgs.add("-XaddExports:java.base/sun.net.www.protocol.jar=ALL-UNNAMED");
          myArgs.add("-XaddExports:java.base/sun.net.www.protocol.mailto=ALL-UNNAMED");
          myArgs.add("-XaddExports:javafx.graphics/com.sun.glass.ui=ALL-UNNAMED");
          myArgs.add("-XaddExports:javafx.web/com.sun.javafx.webkit=ALL-UNNAMED");
          myArgs.add("-XaddExports:javafx.web/com.sun.webkit=ALL-UNNAMED");
        }
        myArgs.add("-Djava.io.tmpdir=" + tmpDir.getAbsolutePath());
        myArgs.add("-Djava.rmi.server.hostname=" + settings.host());
        myArgs.addAll(settings.javaOptions());
        myArgs.add(JBrowserDriverServer.class.getName());
        myArgs.add(Long.toString(portGroup.child));
        myArgs.add(Long.toString(portGroup.parent));
        myArgs.add(Long.toString(portGroup.parentAlt));
        try {
          new ProcessExecutor()
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
                  if (line != null && !line.isEmpty()) {
                    if (!done) {
                      synchronized (ready) {
                        if (line.startsWith("ready on ports ")) {
                          String[] parts = line.substring("ready on ports ".length()).split("/");
                          actualPortGroup.set(new PortGroup(
                              Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                          logPrefix.set(new StringBuilder()
                              .append("[Instance ")
                              .append(sessionIdCounter.incrementAndGet())
                              .append("][Port ")
                              .append(actualPortGroup.get().child)
                              .append("]")
                              .toString());
                          ready.set(true);
                          ready.notifyAll();
                          done = true;
                        } else {
                          log(settings.logger(), logPrefix.get(), line);
                        }
                      }
                    } else {
                      log(settings.logger(), logPrefix.get(), line);
                    }
                  }
                }
              })
              .redirectError(new LogOutputStream() {
                @Override
                protected void processLine(String line) {
                  log(settings.logger(), logPrefix.get(), line);
                }
              })
              .destroyOnExit()
              .command(myArgs).execute();
        } catch (Throwable t) {
          Util.handleException(t);
        }
        synchronized (ready) {
          ready.set(true);
          ready.notifyAll();
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
    return logPrefix.get();
  }

  private static void log(Logger logger, String prefix, String message) {
    if (logger != null && !filteredLogs.contains(message)) {
      LogRecord record = null;
      if (message.startsWith(">")) {
        String[] parts = message.substring(1).split("/", 3);
        record = new LogRecord(Level.parse(parts[0]),
            new StringBuilder().append(prefix).append(" ").append(parts[2]).toString());
        record.setSourceMethodName(parts[1]);
        record.setSourceClassName(JBrowserDriver.class.getName());
      } else {
        record = new LogRecord(Level.WARNING,
            new StringBuilder().append(prefix).append(" ").append(message).toString());
        record.setSourceMethodName(null);
        record.setSourceClassName(JBrowserDriver.class.getName());
      }
      logger.log(record);
    }
  }

  /**
   * Optionally call this method if you want JavaFX initialized and the browser
   * window opened immediately. Otherwise, initialization will happen lazily.
   */
  public void init() {
    try {
      synchronized (lock.validated()) {
        remote.init();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   * <p>
   * Note: it's not possible to switch between headless and GUI mode. You must quit this browser
   * and create a new instance.
   * 
   * @param settings
   *          New settings to take effect, superseding the original ones
   */
  public void reset(final Settings settings) {
    //TODO clear out tmp files except cache
    try {
      synchronized (lock.validated()) {
        remote.reset(settings);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   * <p>
   * Note: it's not possible to switch between headless and GUI mode. You must quit this browser
   * and create a new instance.
   * 
   * @param capabilities
   *          Capabilities to take effect, superseding the original ones
   */
  public void reset(Capabilities capabilities) {
    //TODO clear out tmp files except cache
    Settings settings = Settings.builder().build(capabilities);
    try {
      synchronized (lock.validated()) {
        remote.reset(settings);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    if (!(capabilities instanceof Serializable)) {
      capabilities = new MutableCapabilities(capabilities);
    }
    try {
      synchronized (lock.validated()) {
        remote.storeCapabilities(capabilities);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   */
  public void reset() {
    try {
      synchronized (lock.validated()) {
        remote.reset();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPageSource() {
    try {
      synchronized (lock.validated()) {
        return remote.getPageSource();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCurrentUrl() {
    try {
      synchronized (lock.validated()) {
        return remote.getCurrentUrl();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * @return Status code of the response
   */
  public int getStatusCode() {
    try {
      synchronized (lock.validated()) {
        return remote.getStatusCode();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return -1;
    }
  }

  /**
   * Waits until requests are completed and idle for a certain
   * amount of time. This type of waiting happens automatically on
   * form submissions, page loads, mouse clicks, and text/keyboard
   * entry, so in those cases there's usually no need to call this
   * method. However, calling this method may be useful when requests
   * are triggered under other circumstances or if a more conservative
   * wait is needed.
   * <p>
   * The behavior of this wait algorithm can be configured by
   * {@link Settings.Builder#ajaxWait(long)} and
   * {@link Settings.Builder#ajaxResourceTimeout(long)}.
   */
  public void pageWait() {
    try {
      synchronized (lock.validated()) {
        remote.pageWait();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() {
    try {
      synchronized (lock.validated()) {
        return remote.getTitle();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void get(final String url) {
    try {
      synchronized (lock.validated()) {
        remote.get(url);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElement(By by) {
    return by.findElement(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElements(By by) {
    return by.findElements(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementById(String id) {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.findElementById(id), this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsById(String id) {
    try {
      List<ElementRemote> elements;
      synchronized (lock.validated()) {
        elements = remote.findElementsById(id);
      }
      return Element.constructList(elements, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByXPath(String expr) {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.findElementByXPath(expr), this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    try {
      List<ElementRemote> elements;
      synchronized (lock.validated()) {
        elements = remote.findElementsByXPath(expr);
      }
      return Element.constructList(elements, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByLinkText(final String text) {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.findElementByLinkText(text), this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByPartialLinkText(String text) {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.findElementByPartialLinkText(text), this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    try {
      List<ElementRemote> elements;
      synchronized (lock.validated()) {
        elements = remote.findElementsByLinkText(text);
      }
      return Element.constructList(elements, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    try {
      List<ElementRemote> elements;
      synchronized (lock.validated()) {
        elements = remote.findElementsByPartialLinkText(text);
      }
      return Element.constructList(elements, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByClassName(String cssClass) {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.findElementByClassName(cssClass), this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    try {
      List<ElementRemote> elements;
      synchronized (lock.validated()) {
        elements = remote.findElementsByClassName(cssClass);
      }
      return Element.constructList(elements, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByName(String name) {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.findElementByName(name), this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByName(String name) {
    try {
      List<ElementRemote> elements;
      synchronized (lock.validated()) {
        elements = remote.findElementsByName(name);
      }
      return Element.constructList(elements, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByCssSelector(String expr) {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.findElementByCssSelector(expr), this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    try {
      List<ElementRemote> elements;
      synchronized (lock.validated()) {
        elements = remote.findElementsByCssSelector(expr);
      }
      return Element.constructList(elements, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByTagName(String tagName) {
    try {
      synchronized (lock.validated()) {
        return Element.constructElement(remote.findElementByTagName(tagName), this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    try {
      List<ElementRemote> elements;
      synchronized (lock.validated()) {
        elements = remote.findElementsByTagName(tagName);
      }
      return Element.constructList(elements, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeAsyncScript(String script, Object... args) {
    try {
      Object result;
      synchronized (lock.validated()) {
        result = remote.executeAsyncScript(script, Element.scriptParams(args));
      }
      return Element.constructObject(result, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeScript(String script, Object... args) {
    try {
      Object result;
      synchronized (lock.validated()) {
        result = remote.executeScript(script, Element.scriptParams(args));
      }
      return Element.constructObject(result, this, lock);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.interactions.Keyboard getKeyboard() {
    try {
      synchronized (lock.validated()) {
        KeyboardRemote keyboard = remote.getKeyboard();
        if (keyboard == null) {
          return null;
        }
        return new Keyboard(keyboard, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.interactions.Mouse getMouse() {
    try {
      synchronized (lock.validated()) {
        MouseRemote mouse = remote.getMouse();
        if (mouse == null) {
          return null;
        }
        return new Mouse(mouse, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Capabilities getCapabilities() {
    try {
      synchronized (lock.validated()) {
        return remote.getCapabilities();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    try {
      synchronized (lock.validated()) {
        remote.close();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    Set<String> handles = getWindowHandles();
    if (handles == null || handles.isEmpty()) {
      quit();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWindowHandle() {
    try {
      synchronized (lock.validated()) {
        return remote.getWindowHandle();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getWindowHandles() {
    try {
      synchronized (lock.validated()) {
        return remote.getWindowHandles();
      }
    } catch (Throwable t) {
      Util.handleException(t);
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
        synchronized (lock.validated()) {
          OptionsRemote optionsRemote = remote.manage();
          if (optionsRemote == null) {
            return null;
          }
          return new com.machinepublishers.jbrowserdriver.Options(optionsRemote, logs, lock);
        }
      } catch (Throwable t) {
        Util.handleException(t);
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
      synchronized (lock.validated()) {
        NavigationRemote navigation = remote.navigate();
        if (navigation == null) {
          return null;
        }
        return new com.machinepublishers.jbrowserdriver.Navigation(navigation, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  private void endProcess() {
    if (processEnded.compareAndSet(false, true)) {
      runningInstances.decrementAndGet();
      lock.expired.set(true);
      final Process proc = process.get();
      if (proc != null) {
        while (proc.isAlive()) {
          try {
            PidProcess pidProcess = Processes.newPidProcess(proc);
            try {
              if (!pidProcess.destroyGracefully().waitFor(10, TimeUnit.SECONDS)) {
                throw new RuntimeException();
              }
            } catch (Throwable t1) {
              if (!pidProcess.destroyForcefully().waitFor(10, TimeUnit.SECONDS)) {
                throw new RuntimeException();
              }
            }
          } catch (Throwable t2) {
            try {
              proc.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            } catch (Throwable t3) {}
          }
        }
      }
      try {
        heartbeatThread.interrupt();
        heartbeatThread.join();   
      } catch (Exception e) {}
      FileUtils.deleteQuietly(tmpDir);
      synchronized (locks) {
        locks.remove(lock);
      }
      synchronized (waiting) {
        portGroupsActive.remove(configuredPortGroup.get());
        waiting.notifyAll();
      }
    }
  }

  private void saveData() {
    try {
      synchronized (lock.validated()) {
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
      }
    } catch (Throwable t) {}
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void quit() {
    saveData();
    try {
      synchronized (lock.validated()) {
        remote.quit();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    } finally {
      endProcess();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TargetLocator switchTo() {
    try {
      synchronized (lock.validated()) {
        TargetLocatorRemote locator = remote.switchTo();
        if (locator == null) {
          return null;
        }
        return new com.machinepublishers.jbrowserdriver.TargetLocator(locator, this, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /*
   * This interface was removed in latest Selenium.
   * It remains here for backwards compatibility.
   * Ignore any warnings about an @Override annotation missing.
   */
  public void kill() {
    endProcess();
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    try {
      byte[] bytes;
      synchronized (lock.validated()) {
        bytes = remote.getScreenshot();
      }
      if (bytes == null) {
        return null;
      }
      return outputType.convertFromPngBytes(bytes);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * @return Temporary directory where cached pages are saved.
   */
  public File cacheDir() {
    try {
      synchronized (lock.validated()) {
        return remote.cacheDir();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * @return Temporary directory where downloaded files are saved.
   */
  public File attachmentsDir() {
    try {
      synchronized (lock.validated()) {
        return remote.attachmentsDir();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * @return Temporary directory where media files are saved.
   */
  public File mediaDir() {
    try {
      synchronized (lock.validated()) {
        return remote.mediaDir();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SessionId getSessionId() {
    return sessionId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorHandler getErrorHandler() {
    return super.getErrorHandler();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandExecutor getCommandExecutor() {
    return super.getCommandExecutor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FileDetector getFileDetector() {
    return super.getFileDetector();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public void perform(Collection<Sequence> actions) {
    Map<String, Object> emptyPauseAction = ImmutableMap.of("duration", 0L, "type", "pause");

    EnumMap<SourceType, List<Map<String, Object>>> mappedActions = new EnumMap<>(SourceType.class);
    for (Sequence sequence : actions) {
      Map<String, Object> sequenceValues = sequence.toJson();
      SourceType sourceType = SourceType.valueOf(((String) sequenceValues.get("type")).toUpperCase());
      mappedActions.put(sourceType, (List<Map<String, Object>>) sequenceValues.get("actions"));
    }

    Element lastProcessedElement = null;
    int sequenceSize = mappedActions.values().iterator().next().size();
    for (int cursor = 0; cursor < sequenceSize; cursor++) {
      int counter = 0;
      for (Map.Entry<SourceType, List<Map<String, Object>>> actionEntry : mappedActions.entrySet()) {
        Map<String, Object> action = actionEntry.getValue().get(cursor);
        if (!emptyPauseAction.equals(action)) {
          String actionType = (String) action.get("type");
          Object executor = chooseExecutor(actionEntry.getKey());
          lastProcessedElement = W3CActions.findActionByType(actionType).perform(executor, lastProcessedElement,
          	  action);
          break;
        }
        if (counter == mappedActions.entrySet().size() - 1) {
          W3CActions.PAUSE.perform(getMouse(), lastProcessedElement, emptyPauseAction);
        } else {
          counter++;
        }
      }
    }
  }

  private Object chooseExecutor(SourceType sourceType) {
    switch (sourceType) {
    case KEY:
      return getKeyboard();
    case POINTER:
      return getMouse();
    default:
      throw new IllegalArgumentException("Source type with name " + sourceType + " is not supported");
    }
  }
}
