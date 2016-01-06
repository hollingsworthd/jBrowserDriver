/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC
 * 
 * Sales and support: ops@machinepublishers.com
 * Updates: https://github.com/MachinePublishers/jBrowserDriver
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

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.internal.FindsByClassName;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.openqa.selenium.internal.Locatable;

class Element implements WebElement, JavascriptExecutor, FindsById, FindsByClassName,
    FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName, FindsByXPath, Locatable {
  private final ElementRemote remote;
  private final Logs logs;

  private Element(ElementRemote remote, Logs logs) {
    this.remote = remote;
    this.logs = logs;
  }

  static List<WebElement> constructList(List<ElementRemote> elements, Logs logs) {
    List<WebElement> ret = new ArrayList<WebElement>();
    if (elements != null) {
      for (ElementRemote element : elements) {
        if (element != null) {
          ret.add(new Element(element, logs));
        }
      }
    }
    return ret;
  }

  static WebElement constructElement(ElementRemote element, Logs logs) {
    if (element == null) {
      return null;
    }
    return new Element(element, logs);
  }

  static Object constructObject(Object obj, Logs logs) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof ElementRemote) {
      return new Element((ElementRemote) obj, logs);
    }
    if (obj instanceof List<?>) {
      List retList = new ArrayList();
      for (Object item : (List) obj) {
        if (item instanceof ElementRemote) {
          retList.add(new Element((ElementRemote) item, logs));
        } else {
          retList.add(item);
        }
      }
      return retList;
    }
    return obj;
  }

  @Override
  public void click() {
    try {
      remote.click();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void submit() {
    try {
      remote.submit();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void sendKeys(final CharSequence... keys) {
    try {
      remote.sendKeys(keys);
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public void clear() {
    try {
      remote.clear();
    } catch (RemoteException e) {
      logs.exception(e);
    }
  }

  @Override
  public String getAttribute(final String attrName) {
    try {
      return remote.getAttribute(attrName);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public String getCssValue(final String name) {
    try {
      return remote.getCssValue(name);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Point getLocation() {
    try {
      return remote.getLocation();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Dimension getSize() {
    try {
      return remote.getSize();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public String getTagName() {
    try {
      return remote.getTagName();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public String getText() {
    try {
      return remote.getText();
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public boolean isDisplayed() {
    try {
      return remote.isDisplayed();
    } catch (RemoteException e) {
      logs.exception(e);
      return false;
    }
  }

  @Override
  public boolean isEnabled() {
    try {
      return remote.isEnabled();
    } catch (RemoteException e) {
      logs.exception(e);
      return false;
    }
  }

  @Override
  public boolean isSelected() {
    try {
      return remote.isSelected();
    } catch (RemoteException e) {
      logs.exception(e);
      return false;
    }
  }

  @Override
  public WebElement findElement(By by) {
    try {
      return constructElement(remote.findElement(by), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElements(By by) {
    try {
      return constructList(remote.findElements(by), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByXPath(final String expr) {
    try {
      return constructElement(remote.findElementByXPath(expr), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByXPath(final String expr) {
    try {
      return constructList(remote.findElementsByXPath(expr), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    try {
      return constructElement(remote.findElementByTagName(tagName), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    try {
      return constructList(remote.findElementsByTagName(tagName), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByCssSelector(final String expr) {
    try {
      return constructElement(remote.findElementByCssSelector(expr), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByCssSelector(final String expr) {
    try {
      return constructList(remote.findElementsByCssSelector(expr), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByName(String name) {
    try {
      return constructElement(remote.findElementByName(name), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    try {
      return constructList(remote.findElementsByName(name), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    try {
      return constructElement(remote.findElementByLinkText(text), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    try {
      return constructElement(remote.findElementByPartialLinkText(text), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    try {
      return constructList(remote.findElementsByLinkText(text), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    try {
      return constructList(remote.findElementsByPartialLinkText(text), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    try {
      return constructElement(remote.findElementByClassName(cssClass), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    try {
      return constructList(remote.findElementsByClassName(cssClass), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public WebElement findElementById(final String id) {
    try {
      return constructElement(remote.findElementById(id), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    try {
      return constructList(remote.findElementsById(id), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return new ArrayList<WebElement>();
    }
  }

  @Override
  public Object executeAsyncScript(final String script, final Object... args) {
    try {
      return constructObject(remote.executeAsyncScript(script, args), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Object executeScript(final String script, final Object... args) {
    try {
      return constructObject(remote.executeScript(script, args), logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public Coordinates getCoordinates() {
    try {
      CoordinatesRemote coords = remote.getCoordinates();
      if (coords == null) {
        return null;
      }
      return new com.machinepublishers.jbrowserdriver.Coordinates(coords, logs);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }

  @Override
  public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
    try {
      byte[] bytes = remote.getScreenshot();
      if (bytes == null) {
        return null;
      }
      return outputType.convertFromPngBytes(bytes);
    } catch (RemoteException e) {
      logs.exception(e);
      return null;
    }
  }
}
