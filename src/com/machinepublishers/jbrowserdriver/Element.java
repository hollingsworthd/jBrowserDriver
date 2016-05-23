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
  private final SocketLock lock;

  private Element(ElementRemote remote, JBrowserDriver driver, SocketLock lock) {
    this.remote = remote;
    this.driver = driver;
    this.lock = lock;
  }

  static List<WebElement> constructList(List<ElementRemote> elements, JBrowserDriver driver, SocketLock lock) {
    List<WebElement> ret = new ArrayList<WebElement>();
    if (elements != null) {
      for (ElementRemote element : elements) {
        if (element != null) {
          ret.add(new Element(element, driver, lock));
        }
      }
    }
    return ret;
  }

  static WebElement constructElement(ElementRemote element, JBrowserDriver driver, SocketLock lock) {
    if (element == null) {
      throw new NoSuchElementException("Element not found.");
    }
    return new Element(element, driver, lock);
  }

  static Object constructObject(Object obj, JBrowserDriver driver, SocketLock lock) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof ElementRemote) {
      return new Element((ElementRemote) obj, driver, lock);
    }
    if (obj instanceof List<?>) {
      List retList = new ArrayList();
      for (Object item : (List) obj) {
        retList.add(constructObject(item, driver, lock));
      }
      return retList;
    }
    if (obj instanceof Map<?, ?>) {
      Map retMap = new LinkedHashMap();
      for (Object key : ((Map) obj).keySet()) {
        retMap.put(key, constructObject(((Map) obj).get(key), driver, lock));
      }
      return retMap;
    }
    return obj;
  }

  void activate() {
    try {
      synchronized (lock) {
        remote.activate();
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  void scriptParam(ElementId id) {
    try {
      synchronized (lock) {
        remote.scriptParam(id);
      }
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
      synchronized (lock) {
        remote.click();
      }
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
      synchronized (lock) {
        remote.submit();
      }
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
      synchronized (lock) {
        remote.sendKeys(keys);
      }
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
      synchronized (lock) {
        remote.clear();
      }
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
      synchronized (lock) {
        return remote.getAttribute(attrName);
      }
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
      synchronized (lock) {
        return remote.getCssValue(name);
      }
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
      synchronized (lock) {
        return remote.remoteGetLocation().toSelenium();
      }
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
      synchronized (lock) {
        return remote.remoteGetSize().toSelenium();
      }
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
      synchronized (lock) {
        return remote.remoteGetRect().toSelenium();
      }
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
      synchronized (lock) {
        return remote.getTagName();
      }
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
      synchronized (lock) {
        return remote.getText();
      }
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
      synchronized (lock) {
        return remote.isDisplayed();
      }
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
      synchronized (lock) {
        return remote.isEnabled();
      }
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
      synchronized (lock) {
        return remote.isSelected();
      }
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
      synchronized (lock) {
        return constructElement(remote.findElement(by), driver, lock);
      }
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElements(by);
      }
      return constructList(elements, driver, lock);
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
      synchronized (lock) {
        return constructElement(remote.findElementByXPath(expr), driver, lock);
      }
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElementsByXPath(expr);
      }
      return constructList(elements, driver, lock);
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
      synchronized (lock) {
        return constructElement(remote.findElementByTagName(tagName), driver, lock);
      }
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElementsByTagName(tagName);
      }
      return constructList(elements, driver, lock);
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
      synchronized (lock) {
        return constructElement(remote.findElementByCssSelector(expr), driver, lock);
      }
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElementsByCssSelector(expr);
      }
      return constructList(elements, driver, lock);
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
      synchronized (lock) {
        return constructElement(remote.findElementByName(name), driver, lock);
      }
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElementsByName(name);
      }
      return constructList(elements, driver, lock);
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
      synchronized (lock) {
        return constructElement(remote.findElementByLinkText(text), driver, lock);
      }
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
      synchronized (lock) {
        return constructElement(remote.findElementByPartialLinkText(text), driver, lock);
      }
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElementsByLinkText(text);
      }
      return constructList(elements, driver, lock);
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElementsByPartialLinkText(text);
      }
      return constructList(elements, driver, lock);
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
      synchronized (lock) {
        return constructElement(remote.findElementByClassName(cssClass), driver, lock);
      }
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElementsByClassName(cssClass);
      }
      return constructList(elements, driver, lock);
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
      synchronized (lock) {
        return constructElement(remote.findElementById(id), driver, lock);
      }
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
      List<ElementRemote> elements;
      synchronized (lock) {
        elements = remote.findElementsById(id);
      }
      return constructList(elements, driver, lock);
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
      Object result;
      synchronized (lock) {
        result = remote.executeAsyncScript(script, Element.scriptParams(args));
      }
      return constructObject(result, driver, lock);
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
      Object result;
      synchronized (lock) {
        result = remote.executeScript(script, Element.scriptParams(args));
      }
      return constructObject(result, driver, lock);
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
      synchronized (lock) {
        CoordinatesRemote coords = remote.getCoordinates();
        if (coords == null) {
          return null;
        }
        return new com.machinepublishers.jbrowserdriver.Coordinates(coords, lock);
      }
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
      byte[] bytes = null;
      synchronized (lock) {
        bytes = remote.getScreenshot();
      }
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

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    synchronized (lock) {
      try {
        return remote.remoteHashCode();
      } catch (Throwable t) {
        Util.handleException(t);
        return 0;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Element) {
      ElementId id = new ElementId();
      ((Element) obj).scriptParam(id);
      synchronized (lock) {
        try {
          return remote.remoteEquals(id);
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    }
    return false;
  }
}
