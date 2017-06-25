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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.FindsByClassName;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;
import org.w3c.dom.html.HTMLOptionElement;

import com.machinepublishers.jbrowserdriver.AppThread.Sync;
import com.machinepublishers.jbrowserdriver.Robot.MouseButton;

import javafx.stage.Stage;
import netscape.javascript.JSObject;

class ElementServer extends RemoteObject implements ElementRemote, WebElement,
    JavascriptExecutor, FindsById, FindsByClassName, FindsByLinkText, FindsByName,
    FindsByCssSelector, FindsByTagName, FindsByXPath {

  private static final String IS_VISIBLE;

  static {
    StringBuilder builder = new StringBuilder();
    builder.append("var me = this;");
    builder.append("(function(){");
    //The following JavaScript is Copyright 2011-2015 Software Freedom Conservancy and Copyright 2004-2011 Selenium committers.
    //Adapted and modified from https://github.com/SeleniumHQ/selenium/blob/master/javascript/selenium-core/scripts/selenium-api.js
    builder.append("var findEffectiveStyle = function(element) {");
    builder.append("  if (element.style == undefined) {");
    builder.append("    return undefined;");
    builder.append("  }");
    builder.append("  if (window.getComputedStyle) {");
    builder.append("    return window.getComputedStyle(element, null);");
    builder.append("  }");
    builder.append("  if (element.currentStyle) {");
    builder.append("    return element.currentStyle;");
    builder.append("  }");
    builder.append("  if (window.document.defaultView && window.document.defaultView.getComputedStyle) {");
    builder.append("    return window.document.defaultView.getComputedStyle(element, null);");
    builder.append("  }");
    builder.append("  return undefined;");
    builder.append("};");
    builder.append("var findEffectiveStyleProperty = function(element, property) {");
    builder.append("  var effectiveStyle = findEffectiveStyle(element);");
    builder.append("  var propertyValue = effectiveStyle[property];");
    builder.append("  if (propertyValue == 'inherit' && element.parentNode.style) {");
    builder.append("    return findEffectiveStyleProperty(element.parentNode, property);");
    builder.append("  }");
    builder.append("  return propertyValue;");
    builder.append("};");
    builder.append("var isDisplayed = function(element) {");
    builder.append("  var display = findEffectiveStyleProperty(element, \"display\");");
    builder.append("  if (display == \"none\") return false;");
    builder.append("  if (element.parentNode.style) {");
    builder.append("    return isDisplayed(element.parentNode);");
    builder.append("  }");
    builder.append("  return true;");
    builder.append("};");
    builder.append("var isVisible = function(element) {");
    builder.append("  if (element.tagName) {");
    builder.append("    var tagName = new String(element.tagName).toLowerCase();");
    builder.append("    if (tagName == \"input\") {");
    builder.append("      if (element.type) {");
    builder.append("        var elementType = new String(element.type).toLowerCase();");
    builder.append("        if (elementType == \"hidden\") {");
    builder.append("          return false;");
    builder.append("        }");
    builder.append("      }");
    builder.append("    }");
    builder.append("  }");
    builder.append("  var visibility = findEffectiveStyleProperty(element, \"visibility\");");
    builder.append("  return (visibility != \"hidden\" && isDisplayed(element));");
    builder.append("};");
    builder.append("return isVisible(me);");
    builder.append("})();");
    IS_VISIBLE = builder.toString();
  }

  private static final String SCROLL_INTO_VIEW;
  static {
    StringBuilder builder = new StringBuilder();
    builder.append("var me = this;");
    builder.append("(function(){");
    builder.append("  var rect = me.getBoundingClientRect();");
    builder.append("  if(rect");
    builder.append("      && (rect.top < 0");
    builder.append("      || rect.left < 0");
    builder.append("      || rect.bottom > window.innerHeight");
    builder.append("      || rect.right > window.innerWidth");
    builder.append("      || rect.bottom > document.documentElement.clientHeight");
    builder.append("      || rect.right > document.documentElement.clientWidth)) {");
    builder.append("    me.scrollIntoView();");
    builder.append("  }");
    builder.append("})();");
    SCROLL_INTO_VIEW = builder.toString();
  }
  private static final Pattern rgb = Pattern.compile(
      "rgb\\(([0-9]{1,3}), ([0-9]{1,3}), ([0-9]{1,3})\\)");
  private static final Map<ElementId, ElementServer> map = new HashMap<ElementId, ElementServer>();

  private final JSObject node;
  private final ContextItem contextItem;
  private final AtomicLong frameId = new AtomicLong();

  ElementServer(final JSObject node, final ContextItem contextItem) throws RemoteException {
    AppThread.exec(contextItem.statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            validate(node, contextItem);
            node.getMember("");
            return null;
          }
        });
    this.node = node;
    this.contextItem = contextItem;
  }

  JSObject node() {
    return node;
  }

  void setFrameId(long frameId) {
    this.frameId.set(frameId);
  }

  long frameId() {
    return frameId.get();
  }

  static ElementServer create(final ContextItem contextItem) {
    final JSObject doc = AppThread.exec(contextItem.statusCode,
        new Sync<JSObject>() {
          @Override
          public JSObject perform() {
            return contextItem.selectedFrameDoc();
          }
        });
    try {
      return new ElementServer(doc, contextItem);
    } catch (RemoteException e) {
      Util.handleException(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void activate() {
    contextItem.selectFrame(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void scriptParam(ElementId id) {
    synchronized (map) {
      map.put(id, this);
    }
  }

  private static void validate(JSObject node, ContextItem contextItem) {
    if (node == null) {
      throw new NoSuchElementException("Element not found or does not exist.");
    }
    JSObject doc = node instanceof Document ? node : (JSObject) ((Node) node).getOwnerDocument();
    if (!contextItem.containsFrame(doc)) {
      throw new StaleElementReferenceException("The page containing the element no longer exists.");
    }
    if (!(Boolean) doc.call("contains", node)) {
      throw new StaleElementReferenceException("The element no longer exists within the page.");
    }
  }

  private void validate(boolean mustBeVisible) {
    validate(node, contextItem);
    if (mustBeVisible && !isDisplayed()) {
      throw new ElementNotVisibleException("Element is not visible.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void click() {
    AppThread.exec(contextItem.statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            validate(false);
            node.eval(SCROLL_INTO_VIEW);
            if (contextItem.context.get().keyboard.get().isShiftPressed()) {
              node.eval(
                  new StringBuilder()
                      .append("this.origOnclick = this.onclick;")
                      .append("this.onclick=function(event){")
                      .append("  this.target='_blank';")
                      .append("  if(event){")
                      .append("    if(event.stopPropagation){")
                      .append("      event.stopPropagation();")
                      .append("    }")
                      .append("  }")
                      .append("  if(this.origOnclick){")
                      .append("    this.origOnclick(event? event: null);")
                      .append("  }")
                      .append("  this.onclick = this.origOnclick;")
                      .append("};").toString());
            }
            return null;
          }
        });

    if (node instanceof HTMLOptionElement) {
      AppThread.exec(contextItem.statusCode,
          new Sync<Object>() {
            @Override
            public Object perform() {
              validate(false);
              try {
                new ElementServer((JSObject) ((HTMLOptionElement) node).getParentNode(), contextItem).click();
              } catch (RemoteException e) {
                Util.handleException(e);
              }
              int index = ((HTMLOptionElement) node).getIndex();
              for (int i = 0; i <= index; i++) {
                contextItem.context.get().robot.get().keysType(Keys.DOWN);
              }
              contextItem.context.get().robot.get().keysType(Keys.SPACE);
              return null;
            }
          });
    } else {
      AppThread.exec(contextItem.statusCode,
          new Sync<Object>() {
            @Override
            public Object perform() {
              validate(true);
              final JSObject obj = (JSObject) node.call("getBoundingClientRect");
              final double top = Double.parseDouble(obj.getMember("top").toString());
              final double left = Double.parseDouble(obj.getMember("left").toString());
              final double bottom = Double.parseDouble(obj.getMember("bottom").toString());
              final double right = Double.parseDouble(obj.getMember("right").toString());
              double clickX = (left + right) / 2d;
              double clickY = (top + bottom) / 2d;
              ElementServer doc = ElementServer.create(contextItem);
              if (!node.equals(doc.node.eval(
                  "(function(){return document.elementFromPoint(" + clickX + "," + clickY + ");})();"))) {
                final Stage stage = contextItem.stage.get();
                final int minX = Math.max(0, (int) Math.floor(left));
                final int maxX = Math.min((int) Math.ceil(stage.getScene().getWidth()), (int) Math.ceil(right));
                final int minY = Math.max(0, (int) Math.floor(top));
                final int maxY = Math.min((int) Math.ceil(stage.getScene().getHeight()), (int) Math.ceil(bottom));
                final int incX = (int) Math.max(1, .05d * (double) (maxX - minX));
                final int incY = (int) Math.max(1, .05d * (double) (maxY - minY));
                for (int x = minX; x <= maxX; x += incX) {
                  boolean found = false;
                  for (int y = minY; y <= maxY; y += incY) {
                    if (node.equals(doc.node.eval(
                        "(function(){return document.elementFromPoint(" + x + "," + y + ");})();"))) {
                      clickX = x;
                      clickY = y;
                      found = true;
                      break;
                    }
                  }
                  if (found) {
                    break;
                  }
                }
              }
              final org.openqa.selenium.Point frameLocation = contextItem.selectedFrameLocation();
              contextItem.context.get().robot.get().mouseMove(clickX + frameLocation.getX(), clickY + frameLocation.getY());
              contextItem.context.get().robot.get().mouseClick(MouseButton.LEFT);
              return null;
            }
          });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void submit() {
    AppThread.exec(contextItem.statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            validate(false);
            contextItem.httpListener.get().resetStatusCode();
            if (node instanceof HTMLInputElement) {
              ((HTMLInputElement) node).getForm().submit();
            } else if (node instanceof HTMLFormElement) {
              ((HTMLFormElement) node).submit();
            }
            return null;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendKeys(final CharSequence... keys) {
    AppThread.exec(contextItem.statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            validate(true);
            node.eval(SCROLL_INTO_VIEW);
            node.call("focus");
            return null;
          }
        });
    final boolean fileChooser = node instanceof HTMLInputElement && "file".equalsIgnoreCase(getAttribute("type"));
    if (fileChooser) {
      click();
    }
    contextItem.context.get().robot.get().keysType(keys);
    if (fileChooser) {
      contextItem.context.get().robot.get().typeEnter();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    AppThread.exec(contextItem.statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            validate(false);
            contextItem.httpListener.get().resetStatusCode();
            node.eval(SCROLL_INTO_VIEW);
            node.call("focus");
            node.eval("this.value='';");
            return null;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAttribute(final String attrName) {
    return AppThread.exec(contextItem.statusCode,
        new Sync<String>() {
          @Override
          public String perform() {
            validate(false);
            Object obj = node.getMember(attrName);
            if (obj != null) {
              String str = obj.toString();
              if (!StringUtils.isEmpty(str) && !"undefined".equals(str)) {
                return str;
              }
            }

            obj = executeScript(new StringBuilder()
                .append("return this.getAttribute('").append(attrName).append("');").toString());
            if (obj != null) {
              String str = obj.toString();
              if (!StringUtils.isEmpty(str)) {
                return str;
              }
            }

            return null;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCssValue(final String name) {
    return AppThread.exec(contextItem.statusCode,
        new Sync<String>() {
          @Override
          public String perform() {
            validate(false);
            return cleanUpCssVal((String) (node.eval(new StringBuilder()
                .append("var me = this;")
                .append("(function(){")
                .append("  return window.getComputedStyle(me).getPropertyValue('")
                .append(name)
                .append("');")
                .append("})();").toString())));
          }
        });
  }

  private static String cleanUpCssVal(String rgbStr) {
    if (rgbStr != null) {
      Matcher matcher = rgb.matcher(rgbStr);
      if (matcher.matches()) {
        return new StringBuilder().append("rgba(").append(matcher.group(1)).append(", ")
            .append(matcher.group(2)).append(", ").append(matcher.group(3)).append(", 1)").toString();
      }
    }
    return rgbStr == null ? "" : rgbStr;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point remoteGetLocation() {
    return AppThread.exec(contextItem.statusCode,
        new Sync<Point>() {
          @Override
          public Point perform() {
            validate(true);
            JSObject obj = (JSObject) node.call("getBoundingClientRect");
            int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
            int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
            return new Point(x + 1, y + 1);
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point getLocation() {
    return remoteGetLocation().toSelenium();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Dimension remoteGetSize() {
    return AppThread.exec(contextItem.statusCode,
        new Sync<Dimension>() {
          @Override
          public Dimension perform() {
            validate(true);
            JSObject obj = (JSObject) node.call("getBoundingClientRect");
            int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
            int y2 = (int) Math.rint(Double.parseDouble(obj.getMember("bottom").toString()));
            int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
            int x2 = (int) Math.rint(Double.parseDouble(obj.getMember("right").toString()));
            return new Dimension(x2 - x, y2 - y);
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Dimension getSize() {
    return remoteGetSize().toSelenium();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Rectangle remoteGetRect() {
    return AppThread.exec(contextItem.statusCode,
        new Sync<Rectangle>() {
          @Override
          public Rectangle perform() {
            validate(true);
            JSObject obj = (JSObject) node.call("getBoundingClientRect");
            int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
            int y2 = (int) Math.rint(Double.parseDouble(obj.getMember("bottom").toString()));
            int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
            int x2 = (int) Math.rint(Double.parseDouble(obj.getMember("right").toString()));
            return new Rectangle(x + 1, y + 1, y2 - y, x2 - x);
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Rectangle getRect() {
    return remoteGetRect().toSelenium();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTagName() {
    return getAttribute("tagName").toLowerCase();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getText() {
    return AppThread.exec(contextItem.statusCode,
        new Sync<String>() {
          @Override
          public String perform() {
            validate(false);
            if ((Boolean) node.eval(IS_VISIBLE)) {
              String textAttribute = "TEXTAREA".equals(node.getMember("tagName")) ? "textContent" : "innerText";
              Object text = node.getMember(textAttribute);
              return text instanceof String ? ((String) text).trim() : "";
            }
            return "";
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisplayed() {
    return AppThread.exec(contextItem.statusCode,
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            validate(false);
            return (Boolean) node.eval(IS_VISIBLE);
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEnabled() {
    return AppThread.exec(contextItem.statusCode,
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            validate(false);
            String val = node.getMember("disabled").toString();
            return val == null || "undefined".equals(val) || val.isEmpty() || "false".equals(val);
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSelected() {
    return AppThread.exec(contextItem.statusCode,
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            validate(false);
            String selected = node.getMember("selected").toString();
            String checked = node.getMember("checked").toString();
            return (selected != null && !"undefined".equals(selected) && !"false".equals(selected) && !selected.isEmpty())
                || (checked != null && !"undefined".equals(checked) && !"false".equals(checked) && !checked.isEmpty());
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElement(By by) {
    return (ElementServer) by.findElement(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElements(By by) {
    return by.findElements(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByXPath(final String expr) {
    List list = findElementsByXPath(expr);
    return list.isEmpty() ? null : (ElementServer) list.get(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByXPath(final String expr) {
    return AppThread.exec(contextItem.statusCode,
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            validate(false);
            return asList(executeScript(new StringBuilder()
                .append("var iter = ")
                .append("  document.evaluate(arguments[0], arguments[1], null, XPathResult.ORDERED_NODE_ITERATOR_TYPE);")
                .append("var items = [];")
                .append("var cur = null;")
                .append("while(cur = iter.iterateNext()){")
                .append("  items.push(cur);")
                .append("}")
                .append("return items;").toString(), expr, node));
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByTagName(String tagName) {
    List<ElementServer> list = byTagName(tagName);
    return list == null || list.isEmpty() ? null : list.get(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByTagName(String tagName) {
    return byTagName(tagName);
  }

  private List byTagName(final String tagName) {
    return AppThread.exec(contextItem.statusCode,
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            validate(false);
            if (node != null) {
              return asList(parseScriptResult(
                  node.call("getElementsByTagName", new Object[] { tagName })));
            }
            return new ArrayList<ElementServer>();
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByCssSelector(final String expr) {
    return AppThread.exec(contextItem.statusCode,
        new Sync<ElementServer>() {
          @Override
          public ElementServer perform() {
            validate(false);
            JSObject result = (JSObject) node.call("querySelector", new Object[] { expr });
            if (result == null) {
              return null;
            }
            try {
              return new ElementServer(result, contextItem);
            } catch (RemoteException e) {
              Util.handleException(e);
              return null;
            }
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByCssSelector(final String expr) {
    return AppThread.exec(contextItem.statusCode,
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            validate(false);
            List<ElementServer> elements = new ArrayList<ElementServer>();
            JSObject result = (JSObject) node.call("querySelectorAll", new Object[] { expr });
            for (int i = 0;; i++) {
              Object cur = result.getSlot(i);
              if (cur instanceof Node) {
                try {
                  elements.add(new ElementServer((JSObject) cur, contextItem));
                } catch (RemoteException e) {
                  Util.handleException(e);
                }
              } else {
                break;
              }
            }
            return elements;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByName(String name) {
    return findElementByCssSelector(new StringBuilder().append("*[name='").append(name).append("']").toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByName(String name) {
    return findElementsByCssSelector(new StringBuilder().append("*[name='").append(name).append("']").toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByLinkText(final String text) {
    List<ElementServer> list = byLinkText(text, false, false);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByPartialLinkText(String text) {
    List<ElementServer> list = byLinkText(text, false, true);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByLinkText(String text) {
    return byLinkText(text, true, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByPartialLinkText(String text) {
    return byLinkText(text, true, true);
  }

  private List byLinkText(final String text,
      final boolean multiple, final boolean partial) {
    return AppThread.exec(contextItem.statusCode,
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            validate(false);
            List<ElementServer> elements = new ArrayList<ElementServer>();
            List<ElementServer> nodes = (List<ElementServer>) findElementsByTagName("a");
            for (ElementServer cur : nodes) {
              String curText = cur.getText();
              if (curText == null) {
                continue;
              }
              if ((partial && curText.contains(text))
                  || (!partial && curText.equals(text))) {
                elements.add(cur);
                if (!multiple) {
                  break;
                }
              }
            }
            return elements;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementByClassName(String cssClass) {
    List<ElementServer> list = byCssClass(cssClass);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsByClassName(String cssClass) {
    return byCssClass(cssClass);
  }

  private List byCssClass(String cssClass) {
    return asList(executeScript(
        new StringBuilder().append("return this.getElementsByClassName('").append(cssClass).append("');").toString()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementServer findElementById(final String id) {
    return findElementByCssSelector(new StringBuilder("*[id='").append(id).append("']").toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List findElementsById(String id) {
    return findElementsByCssSelector(new StringBuilder().append("*[id='").append(id).append("']").toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeAsyncScript(final String script, final Object... args) {
    final JavascriptNames jsNames = new JavascriptNames();
    script(true, script, args, jsNames);
    long timeoutAt = contextItem.context.get().timeouts.get().getScriptTimeoutMS();
    if (timeoutAt > 0) {
      timeoutAt += System.currentTimeMillis();
    } else {
      timeoutAt = Long.MAX_VALUE;
    }
    int sleep = 1;
    final int sleepBackoff = 2;
    final int sleepMax = 0x101;
    try {
      while (true) {
        sleep = sleep < sleepMax ? sleep * sleepBackoff : sleep;
        try {
          Thread.sleep(sleep);
        } catch (InterruptedException e) {}
        Object result = AppThread.exec(
            contextItem.statusCode,
            new Sync<Object>() {
              @Override
              public Object perform() {
                validate(false);
                return node.eval(new StringBuilder()
                    .append("(function(){return this.")
                    .append(jsNames.callbackVal)
                    .append(";})();").toString());
              }
            });
        if (!(result instanceof String) || !"undefined".equals(result.toString())) {
          Object parsed = parseScriptResult(result);
          if (parsed instanceof List) {
            if (((List) parsed).size() == 0) {
              return null;
            }
            if (((List) parsed).size() == 1) {
              return ((List) parsed).get(0);
            }
          }
          return parsed;
        }
        if (System.currentTimeMillis() > timeoutAt) {
          throw new TimeoutException(
              "Timeout of " +
                  contextItem.context.get().timeouts.get().getScriptTimeoutMS() +
                  "ms reached for waiting async script to complete.");
        }
      }
    } finally {
      AppThread.exec(
          contextItem.statusCode,
          new Sync<Object>() {
            @Override
            public Object perform() {
              validate(false);
              node.eval(new StringBuilder()
                  .append("delete ")
                  .append(jsNames.callbackVal)
                  .append(";").toString());
              return null;
            }
          });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeScript(final String script, final Object... args) {
    return script(false, script, args, new JavascriptNames());
  }

  private static List<ElementServer> asList(Object objToCast) {
    try {
      return (List<ElementServer>) objToCast;
    } catch (ClassCastException e) {
      return new ArrayList<ElementServer>();
    }
  }

  private static class JavascriptNames {
    private final String callbackVal = Util.randomPropertyName();
    private final String callback = Util.randomPropertyName();
    private final String exec = Util.randomPropertyName();
  }

  private Object script(boolean callback, String script, Object[] args, final JavascriptNames jsNames) {
    for (int i = 0; args != null && i < args.length; i++) {
      if (args[i] instanceof ElementId) {
        synchronized (map) {
          args[i] = ((ElementServer) map.remove(args[i])).node;
        }
      }
    }
    return parseScriptResult(AppThread.exec(contextItem.statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            validate(false);
            List<Object> argList = new ArrayList<Object>();
            if (args != null) {
              argList.addAll(Arrays.asList(args));
            }
            try {
              if (callback) {
                argList.add(null);
                node.eval(new StringBuilder().append("(function(){")
                    .append("this.").append(jsNames.callback).append(" = function(){")
                    .append(jsNames.callbackVal).append(" = arguments && arguments.length > 0? arguments[0] : null;")
                    .append("}")
                    .append("}).apply(this);")
                    .append("this.").append(jsNames.exec).append(" = function(){")
                    .append("arguments[arguments.length-1] = this.").append(jsNames.callback).append(";")
                    .append("return (function(){").append(script).append("}).apply(this, arguments);")
                    .append("};").toString());
              } else {
                node.eval(new StringBuilder().append("this.").append(jsNames.exec).append(" = function(){")
                    .append("return (function(){").append(script).append("}).apply(this, arguments);")
                    .append("};").toString());
              }
              return node.call(jsNames.exec, argList.toArray(new Object[0]));
            } catch (Throwable t) {
              return t;
            } finally {
              node.eval(new StringBuilder().append("delete ").append("this.").append(jsNames.exec).append(";").toString());
              if (callback) {
                node.eval(new StringBuilder().append("delete ").append("this.").append(jsNames.callback).append(";").toString());
              }
            }
          }
        }));
  }

  private Object parseScriptResult(final Object obj) {
    return AppThread.exec(contextItem.statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            validate(false);
            AppThread.handleExecutionException(obj);
            if (obj == null || (obj instanceof String && "undefined".equals(obj.toString()))) {
              return null;
            }
            if (obj instanceof Node) {
              try {
                return new ElementServer((JSObject) obj, contextItem);
              } catch (RemoteException e) {
                Util.handleException(e);
                return null;
              }
            }
            if (obj instanceof JSObject) {
              List<Object> list = new ArrayList<Object>();
              boolean isList = false;
              for (int i = 0;; i++) {
                Object cur = ((JSObject) obj).getSlot(i);
                if (cur instanceof String && "undefined".equals(cur.toString())) {
                  break;
                }
                isList = true;
                list.add(parseScriptResult(cur));
              }
              if (isList) {
                return list;
              }
              if ("function".equals(executeScript("return typeof arguments[0];", obj))) {
                return obj.toString();
              }
              if (Boolean.TRUE.equals(executeScript("return Array.isArray(arguments[0]);", obj))) {
                return new ArrayList<Object>();
              }
              List<Object> mapAsList = (List<Object>) executeScript(new StringBuilder()
                  .append("var list = [];")
                  .append("for(var propertyName in arguments[0]){")
                  .append("list.push(propertyName);")
                  .append("var val = arguments[0][propertyName];")
                  .append("list.push(val === undefined? null : val);")
                  .append("}")
                  .append("return list.length > 0? list : undefined;").toString(),
                  obj);
              //TODO ES6 will support Symbol keys
              Map map = new LinkedHashMap();
              for (int i = 0; mapAsList != null && i < mapAsList.size(); i += 2) {
                map.put(mapAsList.get(i).toString(), mapAsList.get(i + 1));
              }
              return map;
            }
            if (obj instanceof Boolean || obj instanceof Long || obj instanceof Double) {
              return obj;
            }
            if (obj instanceof Integer) {
              return new Long((Integer) obj);
            }
            if (obj instanceof Float) {
              return new Double((Float) obj);
            }
            return obj.toString();
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point locate() {
    AppThread.exec(contextItem.statusCode,
        new Sync<Object>() {
          @Override
          public Point perform() {
            validate(false);
            node.eval(SCROLL_INTO_VIEW);
            return null;
          }
        });
    return AppThread.exec(contextItem.statusCode,
        new Sync<Point>() {
          @Override
          public Point perform() {
            validate(true);
            JSObject obj = (JSObject) node.call("getBoundingClientRect");
            double y = Double.parseDouble(obj.getMember("top").toString());
            double x = Double.parseDouble(obj.getMember("left").toString());
            y = y < 0d ? 0d : y;
            x = x < 0d ? 0d : x;
            final org.openqa.selenium.Point frameLocation = contextItem.selectedFrameLocation();
            return new Point((int) Math.rint(x) + 1 + frameLocation.getX(),
                (int) Math.rint(y) + 1 + frameLocation.getY());
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <X> X getScreenshotAs(OutputType<X> arg0) throws WebDriverException {
    LogsServer.instance().warn("Screenshot not supported on jBrowserDriver WebElements");
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] getScreenshot() throws WebDriverException {
    LogsServer.instance().warn("Screenshot not supported on jBrowserDriver WebElements");
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int remoteHashCode() {
    return AppThread.exec(
        contextItem.statusCode,
        new Sync<Integer>() {
          @Override
          public Integer perform() {
            validate(false);
            return node.hashCode();
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean remoteEquals(ElementId id) {
    return AppThread.exec(
        contextItem.statusCode,
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            validate(false);
            ElementServer other;
            synchronized (map) {
              other = map.remove(id);
            }
            other.validate(false);
            return node.equals(other.node);
          }
        });
  }

}
