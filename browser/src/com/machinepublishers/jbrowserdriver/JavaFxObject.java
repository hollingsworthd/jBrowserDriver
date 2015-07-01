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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import com.machinepublishers.browser.Browser.Fatal;

class JavaFxObject {
  private final Object object;

  JavaFxObject(Object object) {
    this.object = object;
  }

  JavaFxObject(Class<?> object) {
    this.object = object;
  }

  Object unwrap() {
    return this.object;
  }

  boolean is(Class<?> type) {
    Class<?> thisType = object.getClass();
    do {
      if (thisType.getName().equals(type.getName())) {
        return true;
      }
      Class<?>[] interfaces = thisType.getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
        if (interfaces[i].getName().equals(type.getName())) {
          return true;
        }
      }
      thisType = thisType.getSuperclass();
    } while (thisType != null);
    return false;
  }

  JavaFxObject field(String fieldName) {
    Throwable firstError = null;
    Class<?> curClass = ((Class) object);
    while (curClass != null) {
      try {
        Field field = curClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object ret = field.get(null);
        return ret == null ? null : new JavaFxObject(ret);
      } catch (Throwable t) {
        firstError = firstError == null ? t : firstError;
      }
      curClass = curClass.getSuperclass();
    }
    throw new Fatal("Failed to get field: " + fieldName, firstError);
  }

  JavaFxObject call(String methodName, Object... params) {
    Class[] paramTypes;
    Object[] paramsAlt;
    if (params == null || params.length == 0) {
      params = null;
      paramsAlt = new Object[1];
      paramTypes = null;
    } else {
      paramsAlt = new Object[params.length + 1];
      paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; i++) {
        params[i] = params[i] instanceof JavaFxObject ? ((JavaFxObject) params[i]).unwrap() : params[i];
        paramsAlt[i] = params[i];
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
          if (methods[i].getName().equals(methodName)) {
            try {
              methods[i].setAccessible(true);
              Object ret = methods[i].invoke(null, params);
              return ret == null ? null : new JavaFxObject(ret);
            } catch (Throwable t) {}
            try {
              Parameter[] declaredParams = methods[i].getParameters();
              if (declaredParams.length == params.length + 1
                  && declaredParams[params.length].isVarArgs()) {
                paramsAlt[params.length] = Array.newInstance(declaredParams[params.length].getType(), 0);
                Object ret = methods[i].invoke(null, paramsAlt);
                return ret == null ? null : new JavaFxObject(ret);
              }
            } catch (Throwable t) {}
          }
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
          if (methods[i].getName().equals(methodName)) {
            try {
              methods[i].setAccessible(true);
              Object ret = methods[i].invoke(object, params);
              return ret == null ? null : new JavaFxObject(ret);
            } catch (Throwable t) {}
            try {
              Parameter[] declaredParams = methods[i].getParameters();
              if (declaredParams.length == params.length + 1
                  && declaredParams[params.length].isVarArgs()) {
                paramsAlt[params.length] = Array.newInstance(declaredParams[params.length].getType(), 0);
                Object ret = methods[i].invoke(object, paramsAlt);
                return ret == null ? null : new JavaFxObject(ret);
              }
            } catch (Throwable t) {}
          }
        }
        curClass = curClass.getSuperclass();
      }
    }
    throw new Fatal("Method call failed: " + methodName, firstError);
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
