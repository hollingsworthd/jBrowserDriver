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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

import org.openqa.selenium.Cookie;

interface OptionsRemote extends Remote {
  void addCookie(Cookie cookie) throws RemoteException;

  void deleteAllCookies() throws RemoteException;

  void deleteCookie(Cookie cookie) throws RemoteException;

  void deleteCookieNamed(String name) throws RemoteException;

  Cookie getCookieNamed(String name) throws RemoteException;

  Set<Cookie> getCookies() throws RemoteException;

  ImeHandlerRemote ime() throws RemoteException;

  LogsRemote logs() throws RemoteException;

  TimeoutsRemote timeouts() throws RemoteException;

  WindowRemote window() throws RemoteException;
}
