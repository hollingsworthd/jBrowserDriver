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
package com.machinepublishers.jbrowserdriver.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.embed.swing.JFXPanel;

import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.ProxyClassLoader;

import com.machinepublishers.jbrowserdriver.Logs;
import com.sun.glass.ui.PlatformFactory;
import com.sun.glass.ui.monocle.MonoclePlatformFactory;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.glass.ui.monocle.headless.HeadlessPlatform;

public class JavaFx {
  private JavaFx() {}

  private static final Map<Long, JavaFxClassLoader> classLoaders = new HashMap<Long, JavaFxClassLoader>();

  public static synchronized JavaFxObject getNew(Class<?> type, Long id, Object... params) {
    if (!classLoaders.containsKey(id)) {
      classLoaders.put(id, newClassLoader());
    }
    Throwable firstError = null;
    try {
      Class loaded = classLoaders.get(id).loadClass(type.getName());
      if (params == null || params.length == 0) {
        return new JavaFxObject(loaded.newInstance());
      }
      Class[] paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; i++) {
        paramTypes[i] = params[i].getClass();
      }
      JavaFxObject.unbox(paramTypes);
      try {
        return new JavaFxObject(loaded.getConstructor(paramTypes).newInstance(params));
      } catch (Throwable t) {
        firstError = firstError == null ? t : firstError;
        Constructor[] constructors = loaded.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
          try {
            return new JavaFxObject(constructors[i].newInstance(params));
          } catch (Throwable t2) {}
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    throw new IllegalStateException("Could not construct " + type.getName(), firstError);
  }

  public static synchronized JavaFxObject getStatic(Class<?> type, Long id) {
    try {
      if (!classLoaders.containsKey(id)) {
        classLoaders.put(id, newClassLoader());
      }
      return new JavaFxObject(classLoaders.get(id).loadClass(type.getName()));
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    }
  }

  private static JavaFxClassLoader newClassLoader() {
    try {
      JavaFxClassLoader classLoader = new JavaFxClassLoader();
      initToolkit(classLoader);
      return classLoader;
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    }
  }

  public static void initToolkit(ClassLoader classLoader) {
    try {
      if (Settings.headless()) {
        Class<?> platformFactory = classLoader.loadClass(PlatformFactory.class.getName());
        Field field = platformFactory.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(platformFactory, classLoader.loadClass(
            MonoclePlatformFactory.class.getName()).newInstance());

        platformFactory = classLoader.loadClass(NativePlatformFactory.class.getName());
        field = platformFactory.getDeclaredField("platform");
        field.setAccessible(true);
        field.set(platformFactory, classLoader.loadClass(
            HeadlessPlatform.class.getName()).newInstance());
      }
    } catch (Throwable t) {
      //Later versions renamed these fields and also made setting them unnecessary, so ignore exception.
    }
    try {
      classLoader.loadClass(JFXPanel.class.getName()).newInstance();
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  private static class JavaFxClassLoader extends JarClassLoader {
    public JavaFxClassLoader() {
      if (Settings.headless()) {
        super.add(JavaFx.class.getResource("/"));

        Set<File> files = new HashSet<File>();
        files.add(new File(System.getProperty("java.home")));
        for (boolean found = true; found;) {
          Set<File> filesTmp = new HashSet<File>(files);
          found = false;
          for (File file : filesTmp) {
            if (file.isDirectory()) {
              found = true;
              files.remove(file);
              File[] curFiles = file.listFiles();
              for (int i = 0; i < curFiles.length; i++) {
                String name = curFiles[i].getName();
                if (curFiles[i].isDirectory()
                    || ((name.endsWith(".so")
                        || name.endsWith(".a")
                        || name.endsWith(".dll")
                        || name.endsWith(".jar"))
                    && (name.contains("jfx")
                        || name.contains("java")
                        || name.contains("prism")
                        || name.contains("webkit")))) {
                  files.add(curFiles[i]);
                }
              }
            }
          }
        }
        try {
          Path tmpDir = Files.createTempDirectory("jbd");
          tmpDir.toFile().deleteOnExit();
          File jarDir = new File(tmpDir.toFile(), "jars");
          jarDir.mkdir();
          for (File file : files) {
            try {
              File tmpFile;
              if (file.getName().endsWith(".jar")) {
                tmpFile = new File(jarDir, file.getName());
                Files.copy(file.toPath(), tmpFile.toPath());
              } else {
                File libDir = new File(tmpDir.toString(), file.getParentFile().getName());
                libDir.mkdir();
                tmpFile = new File(libDir, file.getName());
                Files.copy(file.toPath(), tmpFile.toPath());
              }
              super.add(tmpFile.toURI().toURL());
            } catch (FileAlreadyExistsException e) {} catch (Throwable t) {
              Logs.exception(t);
            }
          }
        } catch (Throwable t) {
          Logs.exception(t);
        }
      }
    }

    @Override
    public synchronized Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
      if (!className.startsWith("com.machinepublishers.")
          || className.contains(".Dynamic")) {
        return super.loadClass(className, resolveIt);
      }
      return super.getParent().loadClass(className);
    }

    @Override
    public synchronized void initialize() {
      super.initialize();
    }

    @Override
    public synchronized void addAll(Object[] sources) {
      super.addAll(sources);
    }

    @Override
    public synchronized void addAll(List sources) {
      super.addAll(sources);
    }

    @Override
    public synchronized void add(Object source) {
      super.add(source);
    }

    @Override
    public synchronized void add(String resourceName) {
      super.add(resourceName);
    }

    @Override
    public synchronized void add(InputStream jarStream) {
      super.add(jarStream);
    }

    @Override
    public synchronized void add(URL url) {
      super.add(url);
    }

    @Override
    public synchronized void unloadClass(String className) {
      super.unloadClass(className);
    }

    @Override
    public synchronized char getClassNameReplacementChar() {
      return super.getClassNameReplacementChar();
    }

    @Override
    public synchronized void setClassNameReplacementChar(char classNameReplacementChar) {
      super.setClassNameReplacementChar(classNameReplacementChar);
    }

    @Override
    public synchronized Map<String, byte[]> getLoadedResources() {
      return super.getLoadedResources();
    }

    @Override
    public synchronized ProxyClassLoader getLocalLoader() {
      return super.getLocalLoader();
    }

    @Override
    public synchronized Map<String, Class> getLoadedClasses() {
      return super.getLoadedClasses();
    }

    @Override
    public synchronized void addLoader(ProxyClassLoader loader) {
      super.addLoader(loader);
    }

    @Override
    public synchronized URL getResource(String name) {
      return super.getResource(name);
    }

    @Override
    public synchronized InputStream getResourceAsStream(String name) {
      return super.getResourceAsStream(name);
    }

    @Override
    public synchronized ProxyClassLoader getSystemLoader() {
      return super.getSystemLoader();
    }

    @Override
    public synchronized ProxyClassLoader getParentLoader() {
      return super.getParentLoader();
    }

    @Override
    public synchronized ProxyClassLoader getCurrentLoader() {
      return super.getCurrentLoader();
    }

    @Override
    public synchronized ProxyClassLoader getThreadLoader() {
      return super.getThreadLoader();
    }

    @Override
    public synchronized ProxyClassLoader getOsgiBootLoader() {
      return super.getOsgiBootLoader();
    }

    @Override
    public synchronized void clearAssertionStatus() {
      super.clearAssertionStatus();
    }

    @Override
    public synchronized Enumeration<URL> getResources(String arg0) throws IOException {
      return super.getResources(arg0);
    }

    @Override
    public synchronized void setClassAssertionStatus(String arg0, boolean arg1) {
      super.setClassAssertionStatus(arg0, arg1);
    }

    @Override
    public synchronized void setDefaultAssertionStatus(boolean arg0) {
      super.setDefaultAssertionStatus(arg0);
    }

    @Override
    public synchronized void setPackageAssertionStatus(String arg0, boolean arg1) {
      super.setPackageAssertionStatus(arg0, arg1);
    }

    @Override
    public synchronized boolean equals(Object obj) {
      return super.equals(obj);
    }

    @Override
    public synchronized int hashCode() {
      return super.hashCode();
    }

    @Override
    public synchronized String toString() {
      return super.toString();
    }

    @Override
    public synchronized Class loadClass(String name) throws ClassNotFoundException {
      return super.loadClass(name);
    }
  }
}
