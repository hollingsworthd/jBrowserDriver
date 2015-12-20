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
import java.util.Set;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver.ImeHandler;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.logging.Logs;

class Options implements org.openqa.selenium.WebDriver.Options {
  private final OptionsRemote remote;

  Options(OptionsRemote remote) {
    this.remote = remote;
  }

  @Override
  public void addCookie(Cookie cookie) {
    try {
      remote.addCookie(cookie);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void deleteAllCookies() {
    try {
      remote.deleteAllCookies();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void deleteCookie(Cookie cookie) {
    try {
      remote.deleteCookie(cookie);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void deleteCookieNamed(String name) {
    try {
      remote.deleteCookieNamed(name);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public Cookie getCookieNamed(String name) {
    try {
      return remote.getCookieNamed(name);
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Set<Cookie> getCookies() {
    try {
      return remote.getCookies();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public ImeHandler ime() {
    try {
      return new com.machinepublishers.jbrowserdriver.ImeHandler(remote.ime());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Logs logs() {
    //TODO FIXME
    return null;//logs.get();
  }

  @Override
  public Timeouts timeouts() {
    try {
      return new com.machinepublishers.jbrowserdriver.Timeouts(remote.timeouts());
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Window window() {
    try {
      return new com.machinepublishers.jbrowserdriver.Window(remote.window());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }
}
