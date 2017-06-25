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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;

interface ElementRemote extends Remote {
  void activate() throws RemoteException;

  void scriptParam(ElementId id) throws RemoteException;

  void click() throws RemoteException;

  void submit() throws RemoteException;

  void sendKeys(final CharSequence... keys) throws RemoteException;

  void clear() throws RemoteException;

  String getAttribute(final String attrName) throws RemoteException;

  String getCssValue(final String name) throws RemoteException;

  Point remoteGetLocation() throws RemoteException;

  Dimension remoteGetSize() throws RemoteException;

  Rectangle remoteGetRect() throws RemoteException;

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

  Point locate() throws RemoteException;

  byte[] getScreenshot() throws WebDriverException, RemoteException;

  int remoteHashCode() throws RemoteException;

  boolean remoteEquals(ElementId id) throws RemoteException;
}
