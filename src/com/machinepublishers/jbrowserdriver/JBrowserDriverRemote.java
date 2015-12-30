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
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;

interface JBrowserDriverRemote extends Remote {
  void setUp(final Settings settings) throws RemoteException;

  void init() throws RemoteException;

  void reset(Settings settings) throws RemoteException;

  void reset() throws RemoteException;

  int getStatusCode() throws RemoteException;

  void kill() throws RemoteException;

  byte[] getScreenshot() throws WebDriverException, RemoteException;

  CapabilitiesRemote getCapabilities() throws RemoteException;

  KeyboardRemote getKeyboard() throws RemoteException;

  MouseRemote getMouse() throws RemoteException;

  ElementRemote findElementByXPath(String using) throws RemoteException;

  List<ElementRemote> findElementsByXPath(String using) throws RemoteException;

  ElementRemote findElementByTagName(String using) throws RemoteException;

  List<ElementRemote> findElementsByTagName(String using) throws RemoteException;

  ElementRemote findElementByCssSelector(String using) throws RemoteException;

  List<ElementRemote> findElementsByCssSelector(String using) throws RemoteException;

  ElementRemote findElementByName(String using) throws RemoteException;

  List<ElementRemote> findElementsByName(String using) throws RemoteException;

  ElementRemote findElementByLinkText(String using) throws RemoteException;

  List<ElementRemote> findElementsByLinkText(String using) throws RemoteException;

  ElementRemote findElementByPartialLinkText(String using) throws RemoteException;

  List<ElementRemote> findElementsByPartialLinkText(String using) throws RemoteException;

  ElementRemote findElementByClassName(String using) throws RemoteException;

  List<ElementRemote> findElementsByClassName(String using) throws RemoteException;

  ElementRemote findElementById(String using) throws RemoteException;

  List<ElementRemote> findElementsById(String using) throws RemoteException;

  Object executeScript(String script, Object... args) throws RemoteException;

  Object executeAsyncScript(String script, Object... args) throws RemoteException;

  void get(String url) throws RemoteException;

  String getCurrentUrl() throws RemoteException;

  String getTitle() throws RemoteException;

  List<ElementRemote> findElements(By by) throws RemoteException;

  ElementRemote findElement(By by) throws RemoteException;

  String getPageSource() throws RemoteException;

  void close() throws RemoteException;

  void quit() throws RemoteException;

  Set<String> getWindowHandles() throws RemoteException;

  String getWindowHandle() throws RemoteException;

  TargetLocatorRemote switchTo() throws RemoteException;

  NavigationRemote navigate() throws RemoteException;

  OptionsRemote manage() throws RemoteException;

  LogsRemote logs() throws RemoteException;

}
