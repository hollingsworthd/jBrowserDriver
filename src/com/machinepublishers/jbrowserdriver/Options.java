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
import java.util.Set;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver.ImeHandler;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.logging.Logs;

class Options implements org.openqa.selenium.WebDriver.Options {
  private final OptionsRemote remote;
  private final com.machinepublishers.jbrowserdriver.Logs logs;

  Options(OptionsRemote remote, com.machinepublishers.jbrowserdriver.Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  @Override
  public void addCookie(Cookie cookie) {
    try {
      remote.addCookie(cookie);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void deleteAllCookies() {
    try {
      remote.deleteAllCookies();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void deleteCookie(Cookie cookie) {
    try {
      remote.deleteCookie(cookie);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void deleteCookieNamed(String name) {
    try {
      remote.deleteCookieNamed(name);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public Cookie getCookieNamed(String name) {
    try {
      return remote.getCookieNamed(name);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Set<Cookie> getCookies() {
    try {
      return remote.getCookies();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public ImeHandler ime() {
    try {
      ImeHandlerRemote imeHandler = remote.ime();
      if (imeHandler == null) {
        return null;
      }
      return new com.machinepublishers.jbrowserdriver.ImeHandler(imeHandler, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Logs logs() {
    return logs;
  }

  @Override
  public Timeouts timeouts() {
    try {
      TimeoutsRemote timeouts = remote.timeouts();
      if (timeouts == null) {
        return null;
      }
      return new com.machinepublishers.jbrowserdriver.Timeouts(timeouts, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Window window() {
    try {
      WindowRemote window = remote.window();
      if (window == null) {
        return null;
      }
      return new com.machinepublishers.jbrowserdriver.Window(window, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }
}
