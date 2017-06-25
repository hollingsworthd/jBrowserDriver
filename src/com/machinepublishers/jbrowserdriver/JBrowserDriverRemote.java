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

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriverException;

interface JBrowserDriverRemote extends Remote {
  void setUp(final Settings settings) throws RemoteException;

  void storeCapabilities(Capabilities capabilities) throws RemoteException;

  void init() throws RemoteException;

  void reset(Settings settings) throws RemoteException;

  void reset() throws RemoteException;

  int getStatusCode() throws RemoteException;

  void pageWait() throws RemoteException;

  void kill() throws RemoteException;

  byte[] getScreenshot() throws WebDriverException, RemoteException;

  Capabilities getCapabilities() throws RemoteException;

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

  File cacheDir() throws RemoteException;

  File attachmentsDir() throws RemoteException;

  File mediaDir() throws RemoteException;
}
