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

import org.openqa.selenium.security.Credentials;

class Alert implements org.openqa.selenium.Alert {

  private final AlertRemote remote;

  Alert(AlertRemote remote) {
    this.remote = remote;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept() {
    try {
      remote.accept();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dismiss() {
    try {
      remote.dismiss();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getText() {
    try {
      return remote.getText();
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendKeys(String text) {
    try {
      remote.sendKeys(text);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void authenticateUsing(Credentials credentials) {
    //TODO handle basic auth
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setCredentials(Credentials credentials) {
    // TODO handle basic auth
  }
}
