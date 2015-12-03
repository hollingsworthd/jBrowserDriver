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
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.internal.Coordinates;

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

  WebElement findElement(By by) throws RemoteException;

  List<WebElement> findElements(By by) throws RemoteException;

  WebElement findElementByXPath(final String expr) throws RemoteException;

  List<WebElement> findElementsByXPath(final String expr) throws RemoteException;

  WebElement findElementByTagName(String tagName) throws RemoteException;

  List<WebElement> findElementsByTagName(String tagName) throws RemoteException;

  WebElement findElementByCssSelector(final String expr) throws RemoteException;

  List<WebElement> findElementsByCssSelector(final String expr) throws RemoteException;

  WebElement findElementByName(String name) throws RemoteException;

  List<WebElement> findElementsByName(String name) throws RemoteException;

  WebElement findElementByLinkText(final String text) throws RemoteException;

  WebElement findElementByPartialLinkText(String text) throws RemoteException;

  List<WebElement> findElementsByLinkText(String text) throws RemoteException;

  List<WebElement> findElementsByPartialLinkText(String text) throws RemoteException;

  WebElement findElementByClassName(String cssClass) throws RemoteException;

  List<WebElement> findElementsByClassName(String cssClass) throws RemoteException;

  WebElement findElementById(final String id) throws RemoteException;

  List<WebElement> findElementsById(String id) throws RemoteException;

  Object executeAsyncScript(final String script, final Object... args) throws RemoteException;

  Object executeScript(final String script, final Object... args) throws RemoteException;

  Coordinates getCoordinates() throws RemoteException;

  <X> X getScreenshotAs(OutputType<X> arg0) throws WebDriverException, RemoteException;
}
