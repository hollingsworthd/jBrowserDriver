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

import org.openqa.selenium.WebElement;

class TargetLocatorServer extends UnicastRemoteObject implements TargetLocatorRemote,
    org.openqa.selenium.WebDriver.TargetLocator {

  private final JBrowserDriverServer driver;
  private final Context context;

  TargetLocatorServer(JBrowserDriverServer driver, Context context) throws RemoteException {
    this.driver = driver;
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer activeElement() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AlertServer alert() {
    return context.alert.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer defaultContent() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer frame(int index) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer frame(String nameOrId) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer frame(WebElement element) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer frame(ElementRemote element) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer parentFrame() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer window(String windowHandle) {
    context.setCurrent(windowHandle);
    return driver;
  }
}
