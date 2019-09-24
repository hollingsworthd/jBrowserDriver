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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebElement;

class TargetLocatorServer extends RemoteObject implements TargetLocatorRemote,
    org.openqa.selenium.WebDriver.TargetLocator {

  private final JBrowserDriverServer driver;
  private final Context context;

  TargetLocatorServer(JBrowserDriverServer driver, Context context) throws RemoteException {
    this.driver = driver;
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer activeElement() {
    ElementServer element = (ElementServer) driver.executeScript("return document.activeElement;");
    context.item().selectFrame(element);
    return element;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AlertServer alert() {
    return context.alert.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer defaultContent() {
    context.item().deselectFrame();
    return driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer frame(int index) {
    context.item().selectFrame(
        (ElementServer) driver.executeScript(
            "return window.frames[arguments[0]].document;", new Object[] { index }));
    return driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer frame(String nameOrId) {
    List<ElementServer> byName = driver.findElementsByName(nameOrId);
    List<ElementServer> byId = driver.findElementsById(nameOrId);
    List<ElementServer> elements = new ArrayList<ElementServer>();
    elements.addAll(byName);
    elements.addAll(byId);
    for (ElementServer element : elements) {
      if (element.getTagName().equals("frame")
          || element.getTagName().equals("iframe")) {
        element.activate();
        return driver;
      }
    }
    return driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer frame(WebElement element) {
    throw new IllegalStateException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer frame(ElementRemote element) {
    throw new IllegalStateException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer parentFrame() {
    context.item().selectFrame(
        (ElementServer) driver.executeScript("return window.parent.document;"));
    return driver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JBrowserDriverServer window(String windowHandle) {
    context.setCurrent(windowHandle);
    return driver;
  }
}
