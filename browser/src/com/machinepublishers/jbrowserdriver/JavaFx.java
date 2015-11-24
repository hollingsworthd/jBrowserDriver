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

class JavaFx {
  private static JavaFxRemote instance;

  static {
    try {
      instance = new JavaFxServer();
    } catch (RemoteException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  static JavaFxObject getNew(String type, Long id, Object... params) {
    try {
      return new JavaFxObject(instance.getNew(type, id, params));
    } catch (RemoteException e) {
      throw new Fatal(e);
    }
  }

  static JavaFxObject getStatic(String type, Long id) {
    try {
      return new JavaFxObject(instance.getStatic(type, id));
    } catch (RemoteException e) {
      throw new Fatal(e);
    }
  }

  static void close(long settingsId) {
    try {
      instance.close(settingsId);
    } catch (RemoteException e) {
      throw new Fatal(e);
    }
  }
}
