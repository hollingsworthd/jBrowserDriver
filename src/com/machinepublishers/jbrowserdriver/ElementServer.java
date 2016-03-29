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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.FindsByClassName;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.openqa.selenium.internal.Locatable;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.machinepublishers.jbrowserdriver.AppThread.Pause;
import com.machinepublishers.jbrowserdriver.AppThread.Sync;
import com.machinepublishers.jbrowserdriver.Robot.MouseButton;

import netscape.javascript.JSObject;

class ElementServer extends RemoteObject implements ElementRemote, WebElement,
    JavascriptExecutor, FindsById, FindsByClassName, FindsByLinkText, FindsByName,
    FindsByCssSelector, FindsByTagName, FindsByXPath, Locatable {

  private static final String IS_DISPLAYED;

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
    builder.append("var isDisplayed = function(element) {");
    builder.append("  var display = findEffectiveStyleProperty(element, \"display\");");
    builder.append("  if (display == \"none\") return false;");
    builder.append("  if (element.parentNode.style) {");
    builder.append("    return isDisplayed(element.parentNode);");
    builder.append("  }");
    builder.append("  return true;");
    builder.append("};");
    builder.append("return isDisplayed(me);");
    builder.append("})();");
    IS_DISPLAYED = builder.toString();
  }

  private static final Pattern rgb = Pattern.compile(
      "rgb\\(([0-9]{1,3}), ([0-9]{1,3}), ([0-9]{1,3})\\)");
  private static final Map<ElementId, ElementServer> map = new HashMap<ElementId, ElementServer>();

  private final JSObject node;
  private final Context context;

  ElementServer(final JSObject node, final Context context) throws RemoteException {
    AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            //for whatever reason this prevents segfaults due to bugs in the JRE
            node.getMember("");
            return null;
          }
        });
    this.node = node;
    this.context = context;
  }

  static ElementServer create(final Context context) {
    final JSObject doc = AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<JSObject>() {
          @Override
          public JSObject perform() {
            JSObject node;
            if (context.item().frame.get() == null) {
              node = (JSObject) context.item().engine.get().getDocument();
            } else {
              node = context.item().frame.get().node;
            }
            return node;
          }
        });
    try {
      return new ElementServer(doc, context);
    } catch (RemoteException e) {
      LogsServer.instance().exception(e);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void activate() {
    try {
      boolean set = false;
      Object contentWindow = node.getMember("contentWindow");
      if (contentWindow instanceof JSObject) {
        Object document = ((JSObject) contentWindow).getMember("document");
        if (document instanceof JSObject) {
          context.item().frame.set(new ElementServer((JSObject) document, context));
          set = true;
        }
      }
      if (!set) {
        context.item().frame.set(this);
      }
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    }
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void click() {
    AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            node.call("scrollIntoView");
            if (context.keyboard.get().isShiftPressed()) {
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

    AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            JSObject obj = (JSObject) node.call("getBoundingClientRect");
            double y1 = Double.parseDouble(obj.getMember("top").toString());
            double x1 = Double.parseDouble(obj.getMember("left").toString());
            double y2 = Double.parseDouble(obj.getMember("bottom").toString());
            double x2 = Double.parseDouble(obj.getMember("right").toString());
            context.robot.get().mouseMove((x1 + x2) / 2d, (y1 + y2) / 2d);
            context.robot.get().mouseClick(MouseButton.LEFT);
            return null;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void submit() {
    AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            context.item().httpListener.get().resetStatusCode();
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
    AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            node.call("scrollIntoView");
            node.call("focus");
            return null;
          }
        });
    final boolean fileChooser = node instanceof HTMLInputElement && "file".equalsIgnoreCase(getAttribute("type"));
    if (fileChooser) {
      click();
    }
    context.robot.get().keysType(keys);
    if (fileChooser) {
      context.robot.get().keysType(Keys.ENTER);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            context.item().httpListener.get().resetStatusCode();
            node.call("scrollIntoView");
            node.call("focus");
            node.call("setValue", new Object[] { "" });
            return null;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAttribute(final String attrName) {
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<String>() {
          @Override
          public String perform() {
            Object obj = node.getMember(attrName);
            if (obj == null) {
              return "";
            }
            String str = obj.toString();
            return str == null || "undefined".equals(str) ? "" : str;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCssValue(final String name) {
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<String>() {
          @Override
          public String perform() {
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
  public org.openqa.selenium.Point getLocation() {
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<org.openqa.selenium.Point>() {
          @Override
          public org.openqa.selenium.Point perform() {
            JSObject obj = (JSObject) node.call("getBoundingClientRect");
            int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
            int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
            return new org.openqa.selenium.Point(x + 1, y + 1);
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point remoteGetLocation() {
    return new Point(getLocation());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Dimension getSize() {
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<org.openqa.selenium.Dimension>() {
          @Override
          public org.openqa.selenium.Dimension perform() {
            JSObject obj = (JSObject) node.call("getBoundingClientRect");
            int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
            int y2 = (int) Math.rint(Double.parseDouble(obj.getMember("bottom").toString()));
            int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
            int x2 = (int) Math.rint(Double.parseDouble(obj.getMember("right").toString()));
            return new org.openqa.selenium.Dimension(x2 - x, y2 - y);
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Dimension remoteGetSize() {
    return new Dimension(getSize());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Rectangle remoteGetRect() {
    return new Rectangle(getRect());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Rectangle getRect() {
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<org.openqa.selenium.Rectangle>() {
          @Override
          public org.openqa.selenium.Rectangle perform() {
            JSObject obj = (JSObject) node.call("getBoundingClientRect");
            int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
            int y2 = (int) Math.rint(Double.parseDouble(obj.getMember("bottom").toString()));
            int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
            int x2 = (int) Math.rint(Double.parseDouble(obj.getMember("right").toString()));
            return new org.openqa.selenium.Rectangle(x + 1, y + 1, y2 - y, x2 - x);
          }
        });
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
    return getAttribute("textContent");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisplayed() {
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            try {
              //a fast approximation of whether this element is visible
              return (Boolean) node.eval(IS_DISPLAYED);
            } catch (Throwable t) {
              return false;
            }
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEnabled() {
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
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
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            String selected = node.getMember("selected").toString();
            String checked = node.getMember("checked").toString();
            return (selected != null && !"undefined".equals(selected) && !selected.isEmpty())
                || (checked != null && !"undefined".equals(checked) && !checked.isEmpty());
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
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            try {
              return cast(executeScript(new StringBuilder()
                  .append("var iter = ")
                  .append("  document.evaluate(arguments[0], arguments[1], null, XPathResult.ORDERED_NODE_ITERATOR_TYPE);")
                  .append("var items = [];")
                  .append("var cur = null;")
                  .append("while(cur = iter.iterateNext()){")
                  .append("  items.push(cur);")
                  .append("}")
                  .append("return items;").toString(), expr, node),
                  new ArrayList<ElementServer>());
            } catch (Throwable t) {
              LogsServer.instance().exception(t);
            }
            return new ArrayList<ElementServer>();
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
    return AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            if (node != null) {
              return cast(parseScriptResult(
                  node.call("getElementsByTagName", new Object[] { tagName })),
                  new ArrayList<ElementServer>());
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
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<ElementServer>() {
          @Override
          public ElementServer perform() {
            JSObject result = (JSObject) node.call("querySelector", new Object[] { expr });
            if (result == null) {
              return null;
            }
            try {
              return new ElementServer(result, context);
            } catch (RemoteException e) {
              LogsServer.instance().exception(e);
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
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            List<ElementServer> elements = new ArrayList<ElementServer>();
            JSObject result = (JSObject) node.call("querySelectorAll", new Object[] { expr });
            for (int i = 0;; i++) {
              Object cur = result.getSlot(i);
              if (cur instanceof Node) {
                try {
                  elements.add(new ElementServer((JSObject) cur, context));
                } catch (RemoteException e) {
                  LogsServer.instance().exception(e);
                  return new ArrayList<ElementServer>();
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
    return AppThread.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            List<ElementServer> nodes = (List<ElementServer>) findElementsByTagName("a");
            List<ElementServer> elements = new ArrayList<ElementServer>();
            for (ElementServer cur : nodes) {
              if ((partial && cur.getText().contains(text))
                  || (!partial && cur.getText().equals(text))) {
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
    return cast(executeScript(
        new StringBuilder().append("return this.getElementsByClassName('").append(cssClass).append("');").toString()),
        new ArrayList<ElementServer>());
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
    int sleep = 1;
    final int sleepBackoff = 2;
    final int sleepMax = 500;
    while (true) {
      sleep = sleep < sleepMax ? sleep * sleepBackoff : sleep;
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException e) {}
      Object result = AppThread.exec(
          Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
          new Sync<Object>() {
            @Override
            public Object perform() {
              try {
                return node.eval(new StringBuilder()
                    .append("(function(){return this.")
                    .append(jsNames.callbackVal)
                    .append(";})();").toString());
              } finally {
                node.eval(new StringBuilder()
                    .append("delete ")
                    .append(jsNames.callbackVal)
                    .append(";").toString());
              }
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
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object executeScript(final String script, final Object... args) {
    return script(false, script, args, new JavascriptNames());
  }

  private static <T> T cast(Object objToCast, T defaultValueToCastLike) {
    if (objToCast == null || defaultValueToCastLike.getClass().isInstance(objToCast)) {
      return (T) defaultValueToCastLike.getClass().cast(objToCast);
    }
    return (T) defaultValueToCastLike;
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
    return parseScriptResult(AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
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
              context.item().httpListener.get().resetStatusCode();
              return node.call(jsNames.exec, argList.toArray(new Object[0]));
            } catch (Throwable t) {
              return t;
            } finally {
              try {
                node.eval(new StringBuilder().append("delete ").append("this.").append(jsNames.exec).append(";").toString());
                if (callback) {
                  node.eval(new StringBuilder().append("delete ").append("this.").append(jsNames.callback).append(";").toString());
                }
              } catch (Throwable t) {
                LogsServer.instance().exception(t);
              }
            }
          }
        }));
  }

  private Object parseScriptResult(Object obj) {
    if (obj instanceof Throwable) {
      if (obj instanceof RuntimeException) {
        throw new UncheckedExecutionException((RuntimeException) obj);
      }
      throw new UncheckedExecutionException(new RuntimeException((Throwable) obj));
    }
    if (obj == null || (obj instanceof String && "undefined".equals(obj.toString()))) {
      return null;
    }
    if (obj instanceof Node) {
      try {
        return new ElementServer((JSObject) obj, context);
      } catch (RemoteException e) {
        LogsServer.instance().exception(e);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public CoordinatesServer getCoordinates() {
    try {
      return new CoordinatesServer(new org.openqa.selenium.interactions.internal.Coordinates() {

        @Override
        public org.openqa.selenium.Point onScreen() {
          return null;
        }

        @Override
        public org.openqa.selenium.Point onPage() {
          AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
              new Sync<Object>() {
                @Override
                public Point perform() {
                  node.call("scrollIntoView");
                  return null;
                }
              });
          return AppThread.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
              new Sync<org.openqa.selenium.Point>() {
                @Override
                public org.openqa.selenium.Point perform() {
                  JSObject obj = (JSObject) node.call("getBoundingClientRect");
                  double y = Double.parseDouble(obj.getMember("top").toString());
                  double x = Double.parseDouble(obj.getMember("left").toString());
                  y = y < 0d ? 0d : y;
                  x = x < 0d ? 0d : x;
                  return new org.openqa.selenium.Point((int) Math.rint(x) + 1, (int) Math.rint(y) + 1);
                }
              });
        }

        @Override
        public org.openqa.selenium.Point inViewPort() {
          return null;
        }

        @Override
        public Object getAuxiliary() {
          return null;
        }
      });
    } catch (RemoteException e) {
      LogsServer.instance().exception(e);
      return null;
    }
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
}
