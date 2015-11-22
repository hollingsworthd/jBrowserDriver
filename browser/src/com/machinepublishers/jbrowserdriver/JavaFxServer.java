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
import java.io.IOException;
import java.io.InputStream;
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
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.machinepublishers.browser.Browser.Fatal;

class JavaFxServer extends UnicastRemoteObject implements JavaFxRemote {
  JavaFxServer() throws RemoteException {}

  private final Map<Long, ClassLoader> classLoaders = new HashMap<Long, ClassLoader>();
  private final Object lock = new Object();

  public JavaFxObjectRemote getNew(String type, Long id, Object... params) {
    Throwable firstError = null;
    try {
      Class loaded;
      synchronized (lock) {
        if (!classLoaders.containsKey(id)) {
          classLoaders.put(id, newClassLoader());
        }
        loaded = classLoaders.get(id).loadClass(type);
      }
      if (params == null || params.length == 0) {
        Constructor constructor = loaded.getDeclaredConstructor();
        constructor.setAccessible(true);
        return new JavaFxObjectServer(constructor.newInstance());
      }
      Class[] paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; i++) {
        paramTypes[i] = params[i].getClass();
      }
      JavaFxObjectServer.unbox(paramTypes);
      try {
        Constructor constructor = loaded.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return new JavaFxObjectServer(constructor.newInstance(params));
      } catch (Throwable t) {
        firstError = firstError == null ? t : firstError;
        Constructor[] constructors = loaded.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
          try {
            constructors[i].setAccessible(true);
            return new JavaFxObjectServer(constructors[i].newInstance(params));
          } catch (Throwable t2) {}
        }
      }
    } catch (Throwable t) {
      firstError = firstError == null ? t : firstError;
    }
    throw new Fatal("Could not construct " + type, firstError);
  }

  public JavaFxObjectRemote getStatic(String type, Long id) {
    try {
      synchronized (lock) {
        if (!classLoaders.containsKey(id)) {
          classLoaders.put(id, newClassLoader());
        }
        return new JavaFxObjectServer(classLoaders.get(id).loadClass(type));
      }
    } catch (Throwable t) {
      Logs.exception(t);
      return null;
    }
  }

  public void close(long settingsId) {
    synchronized (lock) {
      ClassLoader classLoader = classLoaders.get(settingsId);
      if (classLoader instanceof JavaFxClassLoader) {
        Util.close((JavaFxClassLoader) classLoader);
        JavaFxClassLoader.markForDeletion(((JavaFxClassLoader) classLoader).myTmpDir, true);
      }
    }
  }

  private static ClassLoader newClassLoader() {
    try {
      final ClassLoader classLoader;
      if (Settings.headless()) {
        classLoader = new JavaFxClassLoader(Files.createTempDirectory("jbd").toFile());
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
        Class<?> platformFactory = classLoader.loadClass("com.sun.glass.ui.PlatformFactory");
        Field field = platformFactory.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(platformFactory, classLoader.loadClass(
            "com.sun.glass.ui.monocle.MonoclePlatformFactory").newInstance());

        platformFactory = classLoader.loadClass("com.sun.glass.ui.monocle.NativePlatformFactory");
        field = platformFactory.getDeclaredField("platform");
        field.setAccessible(true);
        Constructor headlessPlatform = classLoader.loadClass("com.sun.glass.ui.monocle.HeadlessPlatform").getDeclaredConstructor();
        headlessPlatform.setAccessible(true);
        field.set(platformFactory, headlessPlatform.newInstance());
      } else {
        try {
          classLoader.loadClass("javafx.embed.swing.JFXPanel").newInstance();
        } catch (Throwable t) {
          Logs.exception(t);
        }
      }
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  private static class JavaFxClassLoader extends URLClassLoader {
    static {
      registerAsParallelCapable();
    }

    private static List<File> list(File dir) {
      File[] children = dir.listFiles();
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

    private static void markForDeletion(File dir, boolean deleteNow) {
      File[] children = dir.listFiles();
      for (int i = 0; children != null && i < children.length; i++) {
        try {
          if (children[i].isFile()) {
            if (deleteNow) {
              children[i].delete();
            } else {
              children[i].deleteOnExit();
            }
          } else {
            markForDeletion(children[i], deleteNow);
          }
        } catch (Throwable t) {
          Logs.exception(t);
        }
      }
      if (deleteNow) {
        dir.delete();
      } else {
        dir.deleteOnExit();
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
      } else {
        File parent = dest.getParentFile();
        if (parent != null) {
          parent.mkdirs();
        }
        Files.copy(source.toPath(), dest.toPath(), options);
      }
    }

    private static URL[] urls(String tmpDir) {
      List<URL> urlList = new ArrayList<URL>();
      try {
        File javaHome = new File(System.getProperty("java.home"));
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
            copy(file, tmpFile);
            urlList.add(tmpFile.toURI().toURL());
          } catch (FileAlreadyExistsException e) {} catch (Throwable t) {
            Logs.exception(t);
          }
        }
        try {
          final URL url = JavaFx.class.getProtectionDomain().getCodeSource().getLocation();
          final URLConnection conn = url.openConnection();
          final File bin = new File(tmpDir, "jbrowserdriver");
          if (conn instanceof JarURLConnection) {
            InputStream stream = ((JarURLConnection) conn).getJarFileURL().openStream();
            try {
              Files.copy(stream, bin.toPath());
            } finally {
              stream.close();
            }
          } else {
            copy(Paths.get(url.toURI()).toFile(), bin.toPath().toFile());
          }
          urlList.add(bin.toURI().toURL());
        } catch (Throwable t) {
          Logs.exception(t);
        }
        File tmpDirFile = new File(tmpDir);
        markForDeletion(tmpDirFile, false);
      } catch (Throwable t) {
        Logs.exception(t);
      }
      return urlList.toArray(new URL[0]);
    }

    private final ClassLoader fallback = ClassLoader.getSystemClassLoader();
    private final File myTmpDir;

    JavaFxClassLoader(File tmpDir) {
      super(urls(tmpDir.getAbsolutePath()), null);
      this.myTmpDir = tmpDir;
    }

    @Override
    public Class loadClass(String className, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(className)) {
        Class c = findLoadedClass(className);
        if (c == null
            && ((!className.startsWith("com.machinepublishers.")
                && !className.startsWith("sun.util.")
                && !className.startsWith("sun.misc.")
                && !className.startsWith("sun.reflect.")) || className.startsWith("com.machinepublishers.jbrowserdriver.Dynamic"))) {
          try {
            c = super.findClass(className);
            if (resolve) {
              super.resolveClass(c);
            }
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
