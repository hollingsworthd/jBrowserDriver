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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;

interface ElementRemote extends Remote {
  void click() throws RemoteException;

  void submit() throws RemoteException;

  void sendKeys(final CharSequence... keys) throws RemoteException;

  void clear() throws RemoteException;

  String getAttribute(final String attrName) throws RemoteException;

  String getCssValue(final String name) throws RemoteException;

  Point getLocation() throws RemoteException;

  Dimension getSize() throws RemoteException;

  String getTagName() throws RemoteException;

  String getText() throws RemoteException;

  boolean isDisplayed() throws RemoteException;

  boolean isEnabled() throws RemoteException;

  boolean isSelected() throws RemoteException;

  ElementRemote findElement(By by) throws RemoteException;

  List<ElementRemote> findElements(By by) throws RemoteException;

  ElementRemote findElementByXPath(final String expr) throws RemoteException;

  List<ElementRemote> findElementsByXPath(final String expr) throws RemoteException;

  ElementRemote findElementByTagName(String tagName) throws RemoteException;

  List<ElementRemote> findElementsByTagName(String tagName) throws RemoteException;

  ElementRemote findElementByCssSelector(final String expr) throws RemoteException;

  List<ElementRemote> findElementsByCssSelector(final String expr) throws RemoteException;

  ElementRemote findElementByName(String name) throws RemoteException;

  List<ElementRemote> findElementsByName(String name) throws RemoteException;

  ElementRemote findElementByLinkText(final String text) throws RemoteException;

  ElementRemote findElementByPartialLinkText(String text) throws RemoteException;

  List<ElementRemote> findElementsByLinkText(String text) throws RemoteException;

  List<ElementRemote> findElementsByPartialLinkText(String text) throws RemoteException;

  ElementRemote findElementByClassName(String cssClass) throws RemoteException;

  List<ElementRemote> findElementsByClassName(String cssClass) throws RemoteException;

  ElementRemote findElementById(final String id) throws RemoteException;

  List<ElementRemote> findElementsById(String id) throws RemoteException;

  Object executeAsyncScript(final String script, final Object... args) throws RemoteException;

  Object executeScript(final String script, final Object... args) throws RemoteException;

  CoordinatesRemote getCoordinates() throws RemoteException;

  byte[] getScreenshot() throws WebDriverException, RemoteException;
}
