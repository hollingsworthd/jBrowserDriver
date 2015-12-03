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
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Keyboard;
import org.openqa.selenium.interactions.Mouse;

interface JBrowserDriverRemote extends Remote {

  void kill() throws RemoteException;

  <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException, RemoteException;

  Capabilities getCapabilities() throws RemoteException;

  Keyboard getKeyboard() throws RemoteException;

  Mouse getMouse() throws RemoteException;

  WebElement findElementByXPath(String using) throws RemoteException;

  List<WebElement> findElementsByXPath(String using) throws RemoteException;

  WebElement findElementByTagName(String using) throws RemoteException;

  List<WebElement> findElementsByTagName(String using) throws RemoteException;

  WebElement findElementByCssSelector(String using) throws RemoteException;

  List<WebElement> findElementsByCssSelector(String using) throws RemoteException;

  WebElement findElementByName(String using) throws RemoteException;

  List<WebElement> findElementsByName(String using) throws RemoteException;

  WebElement findElementByLinkText(String using) throws RemoteException;

  List<WebElement> findElementsByLinkText(String using) throws RemoteException;

  WebElement findElementByPartialLinkText(String using) throws RemoteException;

  List<WebElement> findElementsByPartialLinkText(String using) throws RemoteException;

  WebElement findElementByClassName(String using) throws RemoteException;

  List<WebElement> findElementsByClassName(String using) throws RemoteException;

  WebElement findElementById(String using) throws RemoteException;

  List<WebElement> findElementsById(String using) throws RemoteException;

  Object executeScript(String script, Object... args) throws RemoteException;

  Object executeAsyncScript(String script, Object... args) throws RemoteException;

  void get(String url) throws RemoteException;

  String getCurrentUrl() throws RemoteException;

  String getTitle() throws RemoteException;

  List<WebElement> findElements(By by) throws RemoteException;

  WebElement findElement(By by) throws RemoteException;

  String getPageSource() throws RemoteException;

  void close() throws RemoteException;

  void quit() throws RemoteException;

  Set<String> getWindowHandles() throws RemoteException;

  String getWindowHandle() throws RemoteException;

  org.openqa.selenium.WebDriver.TargetLocator switchTo() throws RemoteException;

  org.openqa.selenium.WebDriver.Navigation navigate() throws RemoteException;

  org.openqa.selenium.WebDriver.Options manage() throws RemoteException;

}
