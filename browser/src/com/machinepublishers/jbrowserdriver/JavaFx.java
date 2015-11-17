/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
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
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.embed.swing.JFXPanel;

import com.machinepublishers.browser.Browser.Fatal;
import com.sun.glass.ui.PlatformFactory;
import com.sun.glass.ui.monocle.MonoclePlatformFactory;
import com.sun.glass.ui.monocle.NativePlatformFactory;

class JavaFx {
  private JavaFx() {}

  private static final Map<Long, ClassLoader> classLoaders = new HashMap<Long, ClassLoader>();
  private static final Object lock = new Object();

  static JavaFxObject getNew(Class<?> type, Long id, Object... params) {
    Throwable firstError = null;
    try {
      Class loaded;
      synchronized (lock) {
        if (!classLoaders.containsKey(id)) {
          classLoaders.put(id, newClassLoader());
        }
        loaded = classLoaders.get(id).loadClass(type.getName());
      }
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
    throw new Fatal("Could not construct " + type.getName(), firstError);
  }

  static JavaFxObject getStatic(Class<?> type, Long id) {
    try {
      synchronized (lock) {
        if (!classLoaders.containsKey(id)) {
          classLoaders.put(id, newClassLoader());
        }
        return new JavaFxObject(classLoaders.get(id).loadClass(type.getName()));
      }
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    }
  }

  static void close(Long id) {
    synchronized (lock) {
      if (classLoaders.get(id) instanceof JavaFxClassLoader) {
        Util.close((JavaFxClassLoader) classLoaders.remove(id));
      }
    }
  }

  private static ClassLoader newClassLoader() {
    try {
      final ClassLoader classLoader;
      if (Settings.headless()) {
        classLoader = new JavaFxClassLoader();
      } else {
        classLoader = JavaFx.class.getClassLoader();
      }
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
        Constructor headlessPlatform =
            classLoader.loadClass("com.sun.glass.ui.monocle.HeadlessPlatform").getDeclaredConstructor();
        headlessPlatform.setAccessible(true);
        field.set(platformFactory, headlessPlatform.newInstance());
      }
    } catch (Throwable t) {
      Logs.exception(t);
    }
    try {
      classLoader.loadClass(JFXPanel.class.getName()).newInstance();
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  private static class JavaFxClassLoader extends URLClassLoader {
    static {
      registerAsParallelCapable();
    }

    private static List<File> list(File dir) {
      File[] children = dir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File file) {
          String name = file.getName();
          return (file.isDirectory()
          || ((name.endsWith(".so")
              || name.endsWith(".a")
              || name.endsWith(".dll")
              || name.endsWith(".jar")
              || name.contains(".properties"))
              && (name.contains("jfx")
              || name.contains("javafx")
              || name.contains(".properties")
              || name.contains("prism")
              || name.contains("webkit")))
          );
        }
      });
      List<File> allFiles = new ArrayList<File>();
      for (int i = 0; children != null && i < children.length; i++) {
        if (children[i].isFile()) {
          allFiles.add(children[i]);
        } else {
          allFiles.addAll(list(children[i]));
        }
      }
      return allFiles;
    }

    private static void deleteAllOnExit(File dir) {
      dir.deleteOnExit();
      File[] children = dir.listFiles();
      for (int i = 0; children != null && i < children.length; i++) {
        if (children[i].isFile()) {
          children[i].deleteOnExit();
        } else {
          deleteAllOnExit(children[i]);
        }
      }
    }

    private static void copy(File source, File dest, CopyOption... options) throws IOException {
      if (source.isDirectory()) {
        dest.mkdirs();
        File[] contents = source.listFiles();
        if (contents != null) {
          for (File file : contents) {
            copy(file, new File(dest.getAbsolutePath() + "/" + file.getName()), options);
          }
        }
      }
      else {
        File parent = dest.getParentFile();
        if (parent != null) {
          parent.mkdirs();
        }
        Files.copy(source.toPath(), dest.toPath(), options);
      }
    }

    private static URL[] urls() {
      List<URL> urlList = new ArrayList<URL>();
      try {
        File javaHome = new File(System.getProperty("java.home"));
        String tmpDir = Files.createTempDirectory("jbd").toFile().getCanonicalPath();
        List<File> files = list(javaHome);
        for (File file : files) {
          try {
            StringBuilder builder = new StringBuilder();
            builder.append(tmpDir);
            builder.append("/");
            List<String> dirParts = new ArrayList<String>();
            for (File cur = file; !cur.getParentFile().equals(javaHome); cur = cur.getParentFile()) {
              dirParts.add(cur.getParentFile().getName());
            }
            for (int i = dirParts.size() - 1; i > -1; i--) {
              builder.append(dirParts.get(i));
              builder.append("/");
            }
            File tmpFileDir = new File(builder.toString());
            tmpFileDir.mkdirs();
            File tmpFile = new File(tmpFileDir, file.getName());
            Files.copy(file.toPath(), tmpFile.toPath());
            urlList.add(tmpFile.toURI().toURL());
          } catch (FileAlreadyExistsException e) {} catch (Throwable t) {
            Logs.exception(t);
          }
        }
        try {
          final URL url = NativePlatformFactory.class.getProtectionDomain().getCodeSource().getLocation();
          final URLConnection conn = url.openConnection();
          final File bin = new File(tmpDir, "jbrowserdriver");
          if (conn instanceof JarURLConnection) {
            Files.copy(((JarURLConnection) conn).getJarFileURL().openStream(), bin.toPath());
          } else {
            copy(Paths.get(url.toURI()).toFile(), bin.toPath().toFile());
          }
          urlList.add(bin.toURI().toURL());
        } catch (Throwable t) {
          Logs.exception(t);
        }
        deleteAllOnExit(new File(tmpDir));
      } catch (Throwable t) {
        Logs.exception(t);
      }
      return urlList.toArray(new URL[0]);
    }

    private final ClassLoader fallback = ClassLoader.getSystemClassLoader();

    JavaFxClassLoader() {
      super(urls(), null);
    }

    @Override
    public Class loadClass(String className, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(className)) {
        Class c = null;
        if (!className.startsWith("com.machinepublishers.")
            || className.startsWith("com.machinepublishers.jbrowserdriver.Dynamic")) {
          try {
            c = super.loadClass(className, resolve);
          } catch (Throwable t) {}
        }
        if (c == null) {
          c = fallback.loadClass(className);
        }
        return c;
      }
    }
  }
}
