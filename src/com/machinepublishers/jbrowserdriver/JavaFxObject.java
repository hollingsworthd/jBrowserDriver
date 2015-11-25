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

import java.rmi.RemoteException;

import com.machinepublishers.browser.Browser.Fatal;

class JavaFxObject {
  private final JavaFxObjectRemote instance;

  JavaFxObject(JavaFxObjectRemote instance) {
    this.instance = instance;
  }

  JavaFxObject(JavaFxObject instance) {
    this.instance = instance.instance;
  }

  Object unwrap() {
    try {
      return instance.unwrap();
    } catch (RemoteException e) {
      throw new Fatal(e);
    }
  }

  boolean is(Class<?> type) {
    try {
      return instance.is(type);
    } catch (RemoteException e) {
      throw new Fatal(e);
    }
  }

  JavaFxObject field(String fieldName) {
    try {
      JavaFxObjectRemote ret = instance.field(fieldName);
      return ret == null ? null : new JavaFxObject(ret);
    } catch (RemoteException e) {
      throw new Fatal(e);
    }
  }

  JavaFxObject call(String methodName, Object... params) {
    try {
      for (int i = 0; params != null && i < params.length; i++) {
        if (params[i] instanceof JavaFxObject) {
          params[i] = ((JavaFxObject) params[i]).instance;
        }
      }
      JavaFxObjectRemote ret = instance.call(methodName, params);
      return ret == null ? null : new JavaFxObject(ret);
    } catch (RemoteException e) {
      throw new Fatal(e);
    }
  }

  @Override
  public String toString() {
    return unwrap().toString();
  }
}
