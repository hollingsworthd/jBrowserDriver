/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
 * https://github.com/MachinePublishers/jBrowserDriver
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
import java.util.Arrays;
import java.util.List;

class ImeHandlerServer extends RemoteObject implements ImeHandlerRemote,
    org.openqa.selenium.WebDriver.ImeHandler {

  protected ImeHandlerServer() throws RemoteException {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void activateEngine(String name) {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void deactivate() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public String getActiveEngine() {
    return "default";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getAvailableEngines() {
    return Arrays.asList(new String[] { "default" });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isActivated() {
    return true;
  }
}
