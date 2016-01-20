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

import org.openqa.selenium.security.Credentials;

class AlertServer extends UnicastRemoteObject implements AlertRemote,
    org.openqa.selenium.Alert {

  private final Context context;

  protected AlertServer(Context context) throws RemoteException {
    super();
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept() {
    context.dialog.get().accept();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dismiss() {
    context.dialog.get().dismiss();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getText() {
    return context.dialog.get().text();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendKeys(String text) {
    context.dialog.get().sendKeys(text);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCredentials(Credentials credentials) {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void authenticateUsing(Credentials arg0) {
    //TODO handle basic auth
  }
}
