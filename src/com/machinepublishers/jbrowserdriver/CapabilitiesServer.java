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
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.Platform;

class CapabilitiesServer extends UnicastRemoteObject implements CapabilitiesRemote,
    org.openqa.selenium.Capabilities {

  protected CapabilitiesServer() throws RemoteException {
    super();
  }

  @Override
  public Map<String, ?> asMap() {
    return new HashMap<String, String>();
  }

  @Override
  public String getBrowserName() {
    return "jBrowserDriver (WebKit-based) by Machine Publishers, LLC";
  }

  @Override
  public Object getCapability(String name) {
    return null;
  }

  @Override
  public Platform getPlatform() {
    return Platform.ANY;
  }

  @Override
  public String getVersion() {
    return Runtime.class.getPackage().getImplementationVersion();
  }

  @Override
  public boolean is(String name) {
    return false;
  }

  @Override
  public boolean isJavascriptEnabled() {
    return true;
  }
}
