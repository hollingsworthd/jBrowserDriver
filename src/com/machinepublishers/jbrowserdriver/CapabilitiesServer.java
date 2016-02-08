/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC
 * 
 * Sales and support: ops@machinepublishers.com
 * Updates: https://github.com/MachinePublishers/jBrowserDriver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, ?> asMap() {
    return new HashMap<String, String>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getBrowserName() {
    return "jBrowserDriver";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getCapability(String name) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Platform getPlatform() {
    return Platform.ANY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    return Runtime.class.getPackage().getImplementationVersion();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean is(String name) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isJavascriptEnabled() {
    return true;
  }
}
