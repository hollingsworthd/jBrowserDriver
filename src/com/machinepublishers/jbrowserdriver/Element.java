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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
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
import org.openqa.selenium.internal.WrapsDriver;

class Element implements WebElement, JavascriptExecutor, FindsById, FindsByClassName,
    FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName, FindsByXPath, Locatable,
    WrapsDriver {
  private final ElementRemote remote;
  private final JBrowserDriver driver;

  private Element(ElementRemote remote, JBrowserDriver driver) {
    this.remote = remote;
    this.driver = driver;
  }

  static List<WebElement> constructList(List<ElementRemote> elements, JBrowserDriver driver) {
    List<WebElement> ret = new ArrayList<WebElement>();
    if (elements != null) {
      for (ElementRemote element : elements) {
        if (element != null) {
          ret.add(new Element(element, driver));
        }
      }
    }
    return ret;
  }

  static WebElement constructElement(ElementRemote element, JBrowserDriver driver) {
    if (element == null) {
      throw new NoSuchElementException("Element not found.");
    }
    return new Element(element, driver);
  }

  static Object constructObject(Object obj, JBrowserDriver driver) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof ElementRemote) {
      return new Element((ElementRemote) obj, driver);
    }
    if (obj instanceof List<?>) {
      List retList = new ArrayList();
      for (Object item : (List) obj) {
        retList.add(constructObject(item, driver));
      }
      return retList;
    }
    if (obj instanceof Map<?, ?>) {
      Map retMap = new LinkedHashMap();
      for (Object key : ((Map) obj).keySet()) {
        retMap.put(key, constructObject(((Map) obj).get(key), driver));
      }
      return retMap;
    }
    return obj;
  }

  void activate() {
    try {
      remote.activate();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  void scriptParam(ElementId id) {
    try {
      remote.scriptParam(id);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void click() {
    try {
      remote.click();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void submit() {
    try {
      remote.submit();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendKeys(final CharSequence... keys) {
    try {
      remote.sendKeys(keys);
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    try {
      remote.clear();
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAttribute(final String attrName) {
    try {
      return remote.getAttribute(attrName);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCssValue(final String name) {
    try {
      return remote.getCssValue(name);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point getLocation() {
    try {
      return remote.remoteGetLocation().toSelenium();
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Dimension getSize() {
    try {
      return remote.remoteGetSize().toSelenium();
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Rectangle getRect() {
    try {
      return remote.remoteGetRect().toSelenium();
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTagName() {
    try {
      return remote.getTagName();
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getText() {
    try {
      return remote.getText();
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisplayed() {
    try {
      return remote.isDisplayed();
    } catch (Throwable t) {
      Util.handleException(t);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEnabled() {
    try {
      return remote.isEnabled();
    } catch (Throwable t) {
      Util.handleException(t);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSelected() {
    try {
      return remote.isSelected();
    } catch (Throwable t) {
      Util.handleException(t);
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElement(By by) {
    try {
      return constructElement(remote.findElement(by), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElements(By by) {
    try {
      return constructList(remote.findElements(by), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByXPath(final String expr) {
    try {
      return constructElement(remote.findElementByXPath(expr), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByXPath(final String expr) {
    try {
      return constructList(remote.findElementsByXPath(expr), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByTagName(String tagName) {
    try {
      return constructElement(remote.findElementByTagName(tagName), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    try {
      return constructList(remote.findElementsByTagName(tagName), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByCssSelector(final String expr) {
    try {
      return constructElement(remote.findElementByCssSelector(expr), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByCssSelector(final String expr) {
    try {
      return constructList(remote.findElementsByCssSelector(expr), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByName(String name) {
    try {
      return constructElement(remote.findElementByName(name), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByName(String name) {
    try {
      return constructList(remote.findElementsByName(name), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByLinkText(final String text) {
    try {
      return constructElement(remote.findElementByLinkText(text), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByPartialLinkText(String text) {
    try {
      return constructElement(remote.findElementByPartialLinkText(text), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    try {
      return constructList(remote.findElementsByLinkText(text), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    try {
      return constructList(remote.findElementsByPartialLinkText(text), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementByClassName(String cssClass) {
    try {
      return constructElement(remote.findElementByClassName(cssClass), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    try {
      return constructList(remote.findElementsByClassName(cssClass), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebElement findElementById(final String id) {
    try {
      return constructElement(remote.findElementById(id), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<WebElement> findElementsById(String id) {
    try {
      return constructList(remote.findElementsById(id), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return new ArrayList<WebElement>();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeAsyncScript(final String script, final Object... args) {
    try {
      return constructObject(remote.executeAsyncScript(script, Element.scriptParams(args)), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeScript(final String script, final Object... args) {
    try {
      return constructObject(remote.executeScript(script, Element.scriptParams(args)), driver);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  static Object[] scriptParams(Object[] args) {
    if (args != null) {
      Object[] argsOut = new Object[args.length];
      for (int i = 0; i < args.length; i++) {
        if (args[i] instanceof Element) {
          ElementId id = new ElementId();
          ((Element) args[i]).scriptParam(id);
          argsOut[i] = id;
        } else {
          argsOut[i] = args[i];
        }
      }
      return argsOut;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Coordinates getCoordinates() {
    try {
      CoordinatesRemote coords = remote.getCoordinates();
      if (coords == null) {
        return null;
      }
      return new com.machinepublishers.jbrowserdriver.Coordinates(coords);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
    try {
      byte[] bytes = remote.getScreenshot();
      if (bytes == null) {
        return null;
      }
      return outputType.convertFromPngBytes(bytes);
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WebDriver getWrappedDriver() {
    return driver;
  }
}
