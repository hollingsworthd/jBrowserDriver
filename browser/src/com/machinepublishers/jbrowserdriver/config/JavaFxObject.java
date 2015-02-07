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

import java.lang.reflect.Method;

public class JavaFxObject {
  private final Object object;

  public JavaFxObject(Object object) {
    this.object = object;
  }

  public JavaFxObject(Class<?> object) {
    this.object = object;
  }

  public Object unwrap() {
    return this.object;
  }

  public boolean is(Class<?> type) {
    Class<?> thisType = object.getClass();
    do {
      if (thisType.getName().equals(type.getName())) {
        return true;
      }
      thisType = thisType.getSuperclass();
    } while (thisType != null);
    return false;
  }

  public JavaFxObject call(String methodName, Object... params) {
    Class[] paramTypes;
    if (params == null || params.length == 0) {
      params = null;
      paramTypes = null;
    } else {
      paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; i++) {
        params[i] = params[i] instanceof JavaFxObject ? ((JavaFxObject) params[i]).unwrap() : params[i];
      }
      paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; i++) {
        paramTypes[i] = params[i] == null ? null : params[i].getClass();
      }
      unbox(paramTypes);
    }
    Throwable firstError = null;
    if (object instanceof Class) {
      Class<?> curClass = ((Class) object);
      while (curClass != null) {
        try {
          Method method = curClass.getDeclaredMethod(methodName, paramTypes);
          method.setAccessible(true);
          Object ret = method.invoke(null, params);
          return ret == null ? null : new JavaFxObject(ret);
        } catch (Throwable t) {
          firstError = firstError == null ? t : firstError;
        }
        Method[] methods = curClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
          try {
            if (methods[i].getName().equals(methodName)) {
              methods[i].setAccessible(true);
              Object ret = methods[i].invoke(null, params);
              return ret == null ? null : new JavaFxObject(ret);
            }
          } catch (Throwable t) {}
        }
        curClass = curClass.getSuperclass();
      }
    } else {
      Class<?> curClass = object.getClass();
      while (curClass != null) {
        try {
          Method method = curClass.getDeclaredMethod(methodName, paramTypes);
          method.setAccessible(true);
          Object ret = method.invoke(object, params);
          return ret == null ? null : new JavaFxObject(ret);
        } catch (Throwable t) {
          firstError = firstError == null ? t : firstError;
        }
        Method[] methods = curClass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
          try {
            if (methods[i].getName().equals(methodName)) {
              methods[i].setAccessible(true);
              Object ret = methods[i].invoke(object, params);
              return ret == null ? null : new JavaFxObject(ret);
            }
          } catch (Throwable t) {}
        }
        curClass = curClass.getSuperclass();
      }
    }
    throw new IllegalStateException("Method call failed: " + methodName, firstError);
  }

  static void unbox(Class[] classes) {
    for (int i = 0; i < classes.length; i++) {
      classes[i] = classes[i] == null ?
          null : (classes[i].equals(Double.class) ?
              double.class : (classes[i].equals(Float.class) ?
                  float.class : (classes[i].equals(Long.class) ?
                      long.class : (classes[i].equals(Integer.class) ?
                          int.class : (classes[i].equals(Boolean.class) ?
                              boolean.class : classes[i])))));
    }
  }

  @Override
  public boolean equals(Object obj) {
    return object.equals(obj);
  }

  @Override
  public int hashCode() {
    return object.hashCode();
  }

  @Override
  public String toString() {
    return object.toString();
  }
}
