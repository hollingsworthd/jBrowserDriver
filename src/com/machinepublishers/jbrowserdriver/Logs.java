/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
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
import java.rmi.registry.LocateRegistry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.logging.LogEntries;

class Logs implements org.openqa.selenium.logging.Logs {
  private static final AtomicReference<Logs> instance = new AtomicReference<Logs>();

  static void init(int id) {
    try {
      instance.set(new Logs((LogsRemote) LocateRegistry.getRegistry(9999).lookup("Logs" + id)));
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  static Logs instance() {
    return instance.get();
  }

  private final LogsRemote remote;

  private Logs(LogsRemote remote) {
    this.remote = remote;
  }

  void clear() {
    try {
      remote.clear();
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  void trace(String message) {
    try {
      remote.trace(message);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  void warn(String message) {
    try {
      remote.warn(message);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  void exception(Throwable t) {
    try {
      remote.exception(t);
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }

  @Override
  public LogEntries get(String s) {
    try {
      return remote.get(s);
    } catch (RemoteException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Set<String> getAvailableLogTypes() {
    try {
      return remote.getAvailableLogTypes();
    } catch (RemoteException e) {
      e.printStackTrace();
      return null;
    }
  }
}
