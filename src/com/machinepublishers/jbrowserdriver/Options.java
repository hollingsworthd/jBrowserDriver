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

import java.util.Set;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver.ImeHandler;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.logging.Logs;

class Options implements org.openqa.selenium.WebDriver.Options {
  private final OptionsRemote remote;
  private final com.machinepublishers.jbrowserdriver.Logs logs;
  private final SocketLock lock;

  Options(OptionsRemote remote, com.machinepublishers.jbrowserdriver.Logs logs, SocketLock lock) {
    this.remote = remote;
    this.logs = logs;
    this.lock = lock;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addCookie(Cookie cookie) {
    try {
      synchronized (lock.validated()) {
        remote.addCookie(cookie);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteAllCookies() {
    try {
      synchronized (lock.validated()) {
        remote.deleteAllCookies();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteCookie(Cookie cookie) {
    try {
      synchronized (lock.validated()) {
        remote.deleteCookie(cookie);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteCookieNamed(String name) {
    try {
      synchronized (lock.validated()) {
        remote.deleteCookieNamed(name);
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Cookie getCookieNamed(String name) {
    try {
      synchronized (lock.validated()) {
        return remote.getCookieNamed(name);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Cookie> getCookies() {
    try {
      synchronized (lock.validated()) {
        return remote.getCookies();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ImeHandler ime() {
    try {
      synchronized (lock.validated()) {
        ImeHandlerRemote imeHandler = remote.ime();
        if (imeHandler == null) {
          return null;
        }
        return new com.machinepublishers.jbrowserdriver.ImeHandler(imeHandler, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Logs logs() {
    return logs;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Timeouts timeouts() {
    try {
      synchronized (lock.validated()) {
        TimeoutsRemote timeouts = remote.timeouts();
        if (timeouts == null) {
          return null;
        }
        return new com.machinepublishers.jbrowserdriver.Timeouts(timeouts, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Window window() {
    try {
      synchronized (lock.validated()) {
        WindowRemote window = remote.window();
        if (window == null) {
          return null;
        }
        return new com.machinepublishers.jbrowserdriver.Window(window, lock);
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }
}
