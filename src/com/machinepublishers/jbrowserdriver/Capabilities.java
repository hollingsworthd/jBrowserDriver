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
import java.util.Map;

import org.openqa.selenium.Platform;

class Capabilities implements org.openqa.selenium.Capabilities {

  private final CapabilitiesRemote remote;
  private final Logs logs;

  Capabilities(CapabilitiesRemote remote, Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  @Override
  public Map<String, ?> asMap() {
    try {
      return remote.asMap();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public String getBrowserName() {
    try {
      return remote.getBrowserName();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Object getCapability(String name) {
    try {
      return remote.getCapability(name);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Platform getPlatform() {
    try {
      return remote.getPlatform();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public String getVersion() {
    try {
      return remote.getVersion();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public boolean is(String name) {
    try {
      return remote.is(name);
    } catch (RemoteException e) {
      logs.exception(e);
      return false;
    }
  }

  @Override
  public boolean isJavascriptEnabled() {
    try {
      return remote.isJavascriptEnabled();
    } catch (RemoteException e) {
      logs.exception(e);
      return false;
    }
  }
}
