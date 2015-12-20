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

  Element(ElementRemote remote) {
    this.remote = remote;
  }

  static List<WebElement> constructList(List<ElementRemote> elements) {
    List<WebElement> ret = new ArrayList<WebElement>(elements.size());
    for (ElementRemote element : elements) {
      ret.add(new Element(element));
    }
    return ret;
  }

  static Object constructObject(Object obj) {
    if (obj instanceof ElementRemote) {
      return new Element((ElementRemote) obj);
    }
    if (obj instanceof List<?>) {
      List retList = new ArrayList();
      for (Object item : (List) obj) {
        if (item instanceof ElementRemote) {
          retList.add(new Element((ElementRemote) item));
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
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void submit() {
    try {
      remote.submit();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void sendKeys(final CharSequence... keys) {
    try {
      remote.sendKeys(keys);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public void clear() {
    try {
      remote.clear();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
    }
  }

  @Override
  public String getAttribute(final String attrName) {
    try {
      return remote.getAttribute(attrName);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public String getCssValue(final String name) {
    try {
      return remote.getCssValue(name);
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Point getLocation() {
    try {
      return remote.getLocation();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Dimension getSize() {
    try {
      return remote.getSize();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public String getTagName() {
    try {
      return remote.getTagName();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public String getText() {
    try {
      return remote.getText();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public boolean isDisplayed() {
    try {
      return remote.isDisplayed();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean isEnabled() {
    try {
      return remote.isEnabled();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean isSelected() {
    try {
      return remote.isSelected();
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public WebElement findElement(By by) {
    //    try {
    //TODO FIXME
    return null;//remote.findElement(by);
    //    } catch (RemoteException e) {
    //      // TODO
    //      e.printStackTrace();
    //      return null;
    //    }
  }

  @Override
  public List<WebElement> findElements(By by) {
    //    try{
    //TODO FIXME
    return null;//remote.findElements(by);
    //    } catch (RemoteException e) {
    //      // TODO
    //      e.printStackTrace();
    //      return null;
    //    }
  }

  @Override
  public WebElement findElementByXPath(final String expr) {
    try {
      return new Element(remote.findElementByXPath(expr));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByXPath(final String expr) {
    try {
      return constructList(remote.findElementsByXPath(expr));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    try {
      return new Element(remote.findElementByTagName(tagName));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    try {
      return constructList(remote.findElementsByTagName(tagName));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByCssSelector(final String expr) {
    try {
      return new Element(remote.findElementByCssSelector(expr));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByCssSelector(final String expr) {
    try {
      return constructList(remote.findElementsByCssSelector(expr));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByName(String name) {
    try {
      return new Element(remote.findElementByName(name));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    try {
      return constructList(remote.findElementsByName(name));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    try {
      return new Element(remote.findElementByLinkText(text));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    try {
      return new Element(remote.findElementByPartialLinkText(text));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    try {
      return constructList(remote.findElementsByLinkText(text));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    try {
      return constructList(remote.findElementsByPartialLinkText(text));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    try {
      return new Element(remote.findElementByClassName(cssClass));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    try {
      return constructList(remote.findElementsByClassName(cssClass));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public WebElement findElementById(final String id) {
    try {
      return new Element(remote.findElementById(id));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    try {
      return constructList(remote.findElementsById(id));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Object executeAsyncScript(final String script, final Object... args) {
    try {
      return constructObject(remote.executeAsyncScript(script, args));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Object executeScript(final String script, final Object... args) {
    try {
      return constructObject(remote.executeScript(script, args));
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Coordinates getCoordinates() {
    try {
      return new com.machinepublishers.jbrowserdriver.Coordinates(remote.getCoordinates());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
    try {
      return outputType.convertFromPngBytes(remote.getScreenshot());
    } catch (RemoteException e) {
      // TODO
      e.printStackTrace();
      return null;
    }
  }
}
