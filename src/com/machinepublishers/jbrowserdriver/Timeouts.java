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
import java.util.concurrent.TimeUnit;

class Timeouts implements org.openqa.selenium.WebDriver.Timeouts {
  private final TimeoutsRemote remote;
  private final LogsServer logs;

  Timeouts(TimeoutsRemote remote, LogsServer logs) {
    this.remote = remote;
    this.logs = logs;
  }

  @Override
  public Timeouts implicitlyWait(long duration, TimeUnit unit) {
    try {
      return new Timeouts(remote.implicitlyWait(duration, unit), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Timeouts pageLoadTimeout(long duration, TimeUnit unit) {
    try {
      return new Timeouts(remote.pageLoadTimeout(duration, unit), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Timeouts setScriptTimeout(long duration, TimeUnit unit) {
    try {
      return new Timeouts(remote.setScriptTimeout(duration, unit), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  long getImplicitlyWaitMS() {
    try {
      return remote.getImplicitlyWaitMS();
    } catch (RemoteException e) {
      logs.exception(e);
      return -1;
    }
  }

  long getPageLoadTimeoutMS() {
    try {
      return remote.getPageLoadTimeoutMS();
    } catch (RemoteException e) {
      logs.exception(e);
      return -1;
    }
  }

  long getScriptTimeoutMS() {
    try {
      return remote.getScriptTimeoutMS();
    } catch (RemoteException e) {
      logs.exception(e);
      return -1;
    }
  }
}
