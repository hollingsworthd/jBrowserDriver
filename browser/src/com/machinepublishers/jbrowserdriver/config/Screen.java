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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javafx.geometry.Rectangle2D;

import com.machinepublishers.jbrowserdriver.Logs;
import com.sun.javafx.Utils;

/**
 * This class is experimental and causes various issues with the GUI toolkit.
 * Recommended that this not be used. Might be removed at any time.
 * Thus, scope of this class is limited to this package.
 * Use BrowserProperties instead.
 */
class Screen {

  private static final Object lock = new Object();
  private static boolean initialized;

  private static void clearScreens() {
    try {
      Field field = javafx.stage.Screen.class.getDeclaredField("screens");
      field.setAccessible(true);
      Utils.getScreenForPoint(1, 1); //this line is required for initialization
      Method method = field.getType().getMethod("clear");
      method.invoke(field.get(null));
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }

  static void addScreen(double width, double height, double dpi) {
    try {
      synchronized (lock) {
        if (!initialized) {
          clearScreens();
          initialized = true;
        }
      }
      javafx.stage.Screen screen = null;
      Constructor c = javafx.stage.Screen.class.getDeclaredConstructor();
      c.setAccessible(true);
      screen = (javafx.stage.Screen) c.newInstance();
      Field field = screen.getClass().getDeclaredField("bounds");
      field.setAccessible(true);
      field.set(screen, new Rectangle2D(0, 0, width, height));
      field = screen.getClass().getDeclaredField("visualBounds");
      field.setAccessible(true);
      field.set(screen, new Rectangle2D(0, 0, width, height));
      field = screen.getClass().getDeclaredField("dpi");
      field.setAccessible(true);
      field.set(screen, dpi);

      field = javafx.stage.Screen.class.getDeclaredField("screens");
      field.setAccessible(true);
      Method method = field.getType().getMethod("add", Object.class);
      method.invoke(field.get(null), screen);
    } catch (Throwable t) {
      Logs.exception(t);
    }
  }
}
