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
package com.machinepublishers.jbrowserdriver;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.embed.swing.JFXPanel;

import com.sun.glass.ui.PlatformFactory;
import com.sun.glass.ui.monocle.MonoclePlatformFactory;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.glass.ui.monocle.headless.HeadlessPlatform;

class JavaFx {
  private JavaFx() {}

  private static final Map<Long, ClassLoader> classLoaders = new HashMap<Long, ClassLoader>();

  static synchronized JavaFxObject getNew(Class<?> type, Long id, Object... params) {
    if (!classLoaders.containsKey(id)) {
      classLoaders.put(id, newClassLoader(id == 1l));
    }
    Throwable firstError = null;
    try {
      Class loaded = classLoaders.get(id).loadClass(type.getName());
      if (params == null || params.length == 0) {
        Constructor constructor = loaded.getDeclaredConstructor();
        constructor.setAccessible(true);
        return new JavaFxObject(constructor.newInstance());
      }
      Class[] paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; i++) {
        paramTypes[i] = params[i].getClass();
      }
      JavaFxObject.unbox(paramTypes);
      try {
        Constructor constructor = loaded.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return new JavaFxObject(constructor.newInstance(params));
      } catch (Throwable t) {
        firstError = firstError == null ? t : firstError;
        Constructor[] constructors = loaded.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
          try {
            constructors[i].setAccessible(true);
            return new JavaFxObject(constructors[i].newInstance(params));
          } catch (Throwable t2) {}
        }
      }
    } catch (Throwable t) {
      firstError = firstError == null ? t : firstError;
    }
    throw new IllegalStateException("Could not construct " + type.getName(), firstError);
  }

  static synchronized JavaFxObject getStatic(Class<?> type, Long id) {
    try {
      if (!classLoaders.containsKey(id)) {
        classLoaders.put(id, newClassLoader(id == 1l));
      }
      return new JavaFxObject(classLoaders.get(id).loadClass(type.getName()));
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    }
  }

  static synchronized void close(Long id) {
    if (classLoaders.get(id) instanceof JavaFxClassLoader) {
      Util.close((JavaFxClassLoader) classLoaders.get(id));
    }
  }

  private static ClassLoader newClassLoader(boolean useCurrentClassLoader) {
    try {
      ClassLoader classLoader = !useCurrentClassLoader && Settings.headless()
          ? new JavaFxClassLoader() : JavaFx.class.getClassLoader();
      initToolkit(classLoader);
      return classLoader;
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    }
  }

  private static void initToolkit(ClassLoader classLoader) {
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
      //Some versions renamed these fields or made setting them unnecessary, so ignore exception.
    }
    try {
      classLoader.loadClass(JFXPanel.class.getName()).newInstance();
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  private static class JavaFxClassLoader extends URLClassLoader {
    private static final URL[] urls;
    private static Map<String, byte[]> classes = new HashMap<String, byte[]>();
    private static ClassLoader defaultClassLoader = JavaFx.class.getClassLoader();
    static {
      try {
        byte[] classTmp = Util.toBytes(JavaFx.class.getResource("/"
            + DynamicHttpListener.class.getName().replace('.', '/') + ".class").openStream());
        classes.put(DynamicHttpListener.class.getName(), classTmp);
      } catch (Throwable t) {
        Logs.exception(t);
      }
      try {
        byte[] classTmp = Util.toBytes(JavaFx.class.getResource("/"
            + DynamicPopupHandler.class.getName().replace('.', '/') + ".class").openStream());
        classes.put(DynamicPopupHandler.class.getName(), classTmp);
      } catch (Throwable t) {
        Logs.exception(t);
      }
      try {
        byte[] classTmp = Util.toBytes(JavaFx.class.getResource("/"
            + DynamicTitleListener.class.getName().replace('.', '/') + ".class").openStream());
        classes.put(DynamicTitleListener.class.getName(), classTmp);
      } catch (Throwable t) {
        Logs.exception(t);
      }
      List<URL> urlList = new ArrayList<URL>();
      urlList.add(JavaFx.class.getResource("./openjfx-monocle.jar"));
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
                      || name.contains("javafx")
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
            urlList.add(tmpFile.toURI().toURL());
          } catch (FileAlreadyExistsException e) {} catch (Throwable t) {
            Logs.exception(t);
          }
        }
      } catch (Throwable t) {
        Logs.exception(t);
      }
      urls = urlList.toArray(new URL[0]);
    }

    JavaFxClassLoader() {
      super(urls, null);
    }

    @Override
    public synchronized Class loadClass(String className) throws ClassNotFoundException {
      Class c = super.findLoadedClass(className);
      if (c == null) {
        if (classes.containsKey(className)) {
          c = defineClass(className, classes.get(className), 0, classes.get(className).length);
        } else {
          try {
            c = super.loadClass(className);
          } catch (Throwable t) {
            c = defaultClassLoader.loadClass(className);
          }
        }
      }
      return c;
    }
  }
}
