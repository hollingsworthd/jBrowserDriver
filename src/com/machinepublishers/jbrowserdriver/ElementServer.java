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
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;

import com.machinepublishers.jbrowserdriver.Robot.MouseButton;
import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

import netscape.javascript.JSObject;

class ElementServer extends UnicastRemoteObject implements ElementRemote, WebElement,
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
  private final AtomicReference<JSObject> node;
  private final Context context;

  ElementServer(final AtomicReference<JSObject> node, final Context context) throws RemoteException {
    try {
      this.node = node;
      this.context = context;
    } catch (Throwable t) {
      throw new BrowserException.Retry(t);
    }
  }

  static ElementServer create(final Context context) {
    final long settingsId = Long.parseLong(context.item().engine.get().getUserAgent());
    final AtomicReference<JSObject> doc = new AtomicReference<JSObject>(
        Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
            new Sync<JSObject>() {
              @Override
              public JSObject perform() {
                return (JSObject) context.item().engine.get().getDocument();
              }
            }, settingsId));
    try {
      return new ElementServer(doc, context);
    } catch (RemoteException e) {
      context.logs.get().exception(e);
      return null;
    }
  }

  @Override
  public void click() {
    Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            node.get().call("scrollIntoView");
            if (context.keyboard.get().isShiftPressed()) {
              node.get().eval(
                  "this.origOnclick = this.onclick;"
                      + "this.onclick=function(event){"
                      + "  this.target='_blank';"
                      + "  if(event){"
                      + "    if(event.stopPropagation){"
                      + "      event.stopPropagation();"
                      + "    }"
                      + "  }"
                      + "  if(this.origOnclick){"
                      + "    this.origOnclick(event? event: null);"
                      + "  }"
                      + "  this.onclick = this.origOnclick;"
                      + "};");
            }
            return null;
          }
        }, context.settingsId.get());

    Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            JSObject obj = (JSObject) node.get().call("getBoundingClientRect");
            double y = Double.parseDouble(obj.getMember("top").toString());
            double x = Double.parseDouble(obj.getMember("left").toString());
            y = y < 0d ? 0d : y;
            x = x < 0d ? 0d : x;
            context.robot.get().mouseMove(x + 1, y + 1);
            context.robot.get().mouseClick(MouseButton.LEFT);
            return null;
          }
        }, context.settingsId.get());
  }

  @Override
  public void submit() {
    Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            context.item().httpListener.get().resetStatusCode();
            if (node.get() instanceof HTMLInputElement) {
              ((HTMLInputElement) node.get()).getForm().submit();
            } else if (node.get() instanceof HTMLFormElement) {
              ((HTMLFormElement) node.get()).submit();
            }
            return null;
          }
        }, context.settingsId.get());
  }

  @Override
  public void sendKeys(final CharSequence... keys) {
    Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            node.get().call("scrollIntoView");
            node.get().call("focus");
            return null;
          }
        }, context.settingsId.get());
    context.robot.get().keysType(keys);
  }

  @Override
  public void clear() {
    Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            context.item().httpListener.get().resetStatusCode();
            node.get().call("scrollIntoView");
            node.get().call("focus");
            node.get().call("setValue", new Object[] { "" });
            return null;
          }
        }, context.settingsId.get());
  }

  @Override
  public String getAttribute(final String attrName) {
    String val = (String) (node.get().getMember(attrName));
    return val == null || val.equals("undefined") ? "" : val;
  }

  @Override
  public String getCssValue(final String name) {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<String>() {
          @Override
          public String perform() {
            return cleanUpCssVal((String) (node.get().eval("var me = this;"
                + "(function(){"
                + "  return window.getComputedStyle(me).getPropertyValue('" + name + "');"
                + "})();")));
          }
        }, context.settingsId.get());
  }

  private static String cleanUpCssVal(String rgbStr) {
    if (rgbStr != null) {
      Matcher matcher = rgb.matcher(rgbStr);
      if (matcher.matches()) {
        return "rgba(" + matcher.group(1) + ", "
            + matcher.group(2) + ", " + matcher.group(3) + ", 1)";
      }
    }
    return rgbStr == null ? "" : rgbStr;
  }

  @Override
  public Point getLocation() {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Point>() {
          @Override
          public Point perform() {
            JSObject obj = (JSObject) node.get().call("getBoundingClientRect");
            int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
            int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
            return new Point(x + 1, y + 1);
          }
        }, context.settingsId.get());
  }

  @Override
  public Dimension getSize() {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Dimension>() {
          @Override
          public Dimension perform() {
            JSObject obj = (JSObject) node.get().call("getBoundingClientRect");
            int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
            int y2 = (int) Math.rint(Double.parseDouble(obj.getMember("bottom").toString()));
            int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
            int x2 = (int) Math.rint(Double.parseDouble(obj.getMember("right").toString()));
            return new Dimension(x2 - x, y2 - y);
          }
        }, context.settingsId.get());
  }

  @Override
  public String getTagName() {
    return getAttribute("tagName");
  }

  @Override
  public String getText() {
    return getAttribute("textContent");
  }

  @Override
  public boolean isDisplayed() {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            try {
              //a fast approximation of whether this element is visible
              return (Boolean) node.get().eval(IS_DISPLAYED);
            } catch (Throwable t) {
              return false;
            }
          }
        }, context.settingsId.get());
  }

  @Override
  public boolean isEnabled() {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            String val = node.get().getMember("disabled").toString();
            return val == null || "undefined".equals(val) || val.isEmpty();
          }
        }, context.settingsId.get());
  }

  @Override
  public boolean isSelected() {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Boolean>() {
          @Override
          public Boolean perform() {
            String selected = node.get().getMember("selected").toString();
            String checked = node.get().getMember("checked").toString();
            return (selected != null && !"undefined".equals(selected) && !selected.isEmpty())
                || (checked != null && !"undefined".equals(checked) && !checked.isEmpty());
          }
        }, context.settingsId.get());
  }

  @Override
  public ElementServer findElement(By by) {
    //TODO FIXME
    return null;//by.findElement(this);
  }

  @Override
  public List findElements(By by) {
    //TODO FIXME
    return null;//by.findElements(this);
  }

  @Override
  public ElementServer findElementByXPath(final String expr) {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<ElementServer>() {
          @Override
          public ElementServer perform() {
            try {
              return new ElementServer(new AtomicReference(XPathFactory.newInstance().newXPath().evaluate(
                  expr, node.get(), XPathConstants.NODE)), context);
            } catch (Throwable t) {
              Logs.logsFor(context.settingsId.get()).exception(t);
            }
            return null;
          }
        }, context.settingsId.get());
  }

  @Override
  public List findElementsByXPath(final String expr) {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            try {
              List<ElementServer> elements = new ArrayList<ElementServer>();
              NodeList list = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                  expr, node.get(), XPathConstants.NODESET);
              for (int i = 0; i < list.getLength(); i++) {
                elements.add(new ElementServer(new AtomicReference(list.item(i)), context));
              }
              return elements;
            } catch (Throwable t) {
              Logs.logsFor(context.settingsId.get()).exception(t);
            }
            return null;
          }
        }, context.settingsId.get());
  }

  @Override
  public ElementServer findElementByTagName(String tagName) {
    List<ElementServer> list = byTagName(tagName);
    return list == null || list.isEmpty() ? null : list.get(0);
  }

  @Override
  public List findElementsByTagName(String tagName) {
    return byTagName(tagName);
  }

  private List byTagName(final String tagName) {
    return Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            if (node.get() != null) {
              return (List<ElementServer>) parseScriptResult(
                  node.get().call("getElementsByTagName", new Object[] { tagName }));
            }
            return null;
          }
        }, context.settingsId.get());
  }

  @Override
  public ElementServer findElementByCssSelector(final String expr) {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<ElementServer>() {
          @Override
          public ElementServer perform() {
            JSObject result = (JSObject) node.get().call("querySelector", new Object[] { expr });
            if (result == null) {
              return null;
            }
            try {
              return new ElementServer(new AtomicReference(result), context);
            } catch (RemoteException e) {
              context.logs.get().exception(e);
              return null;
            }
          }
        }, context.settingsId.get());
  }

  @Override
  public List findElementsByCssSelector(final String expr) {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<List<ElementServer>>() {
          @Override
          public List<ElementServer> perform() {
            List<ElementServer> elements = new ArrayList<ElementServer>();
            JSObject result = (JSObject) node.get().call("querySelectorAll", new Object[] { expr });
            for (int i = 0;; i++) {
              Object cur = result.getSlot(i);
              if (cur instanceof Node) {
                try {
                  elements.add(new ElementServer(new AtomicReference(cur), context));
                } catch (RemoteException e) {
                  context.logs.get().exception(e);
                  return null;
                }
              } else {
                break;
              }
            }
            return elements;
          }
        }, context.settingsId.get());
  }

  @Override
  public ElementServer findElementByName(String name) {
    return findElementByCssSelector("*[name='" + name + "']");
  }

  @Override
  public List findElementsByName(String name) {
    return findElementsByCssSelector("*[name='" + name + "']");
  }

  @Override
  public ElementServer findElementByLinkText(final String text) {
    List<ElementServer> list = byLinkText(text, false, false);
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public ElementServer findElementByPartialLinkText(String text) {
    List<ElementServer> list = byLinkText(text, false, true);
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public List findElementsByLinkText(String text) {
    return byLinkText(text, true, false);
  }

  @Override
  public List findElementsByPartialLinkText(String text) {
    return byLinkText(text, true, true);
  }

  private List byLinkText(final String text,
      final boolean multiple, final boolean partial) {
    return Util.exec(Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
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
        }, context.settingsId.get());
  }

  @Override
  public ElementServer findElementByClassName(String cssClass) {
    List<ElementServer> list = byCssClass(cssClass);
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public List findElementsByClassName(String cssClass) {
    return byCssClass(cssClass);
  }

  private List byCssClass(String cssClass) {
    return (List<ElementServer>) executeScript("return this.getElementsByClassName('" + cssClass + "');");
  }

  @Override
  public ElementServer findElementById(final String id) {
    return findElementByCssSelector("*[id='" + id + "']");
  }

  @Override
  public List findElementsById(String id) {
    return findElementsByCssSelector("*[id='" + id + "']");
  }

  @Override
  public Object executeAsyncScript(final String script, final Object... args) {
    lock();
    try {
      script(true, script, args);
      int sleep = 1;
      final int sleepBackoff = 2;
      final int sleepMax = 500;
      while (true) {
        sleep = sleep < sleepMax ? sleep * sleepBackoff : sleep;
        try {
          Thread.sleep(sleep);
        } catch (InterruptedException e) {}
        Object result = Util.exec(
            Pause.NONE, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
            new Sync<Object>() {
              @Override
              public Object perform() {
                return node.get().eval("(function(){return this.screenslicerCallbackVal;})();");
              }
            }, context.settingsId.get());
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
    } finally {
      unlock();
    }
  }

  @Override
  public Object executeScript(final String script, final Object... args) {
    lock();
    try {
      return script(false, script, args);
    } finally {
      unlock();
    }
  }

  private void lock() {
    long myThread = context.latestThread.incrementAndGet();
    synchronized (context.curThread) {
      while (myThread != context.curThread.get() + 1) {
        try {
          context.curThread.wait();
        } catch (Exception e) {
          context.logs.get().exception(e);
        }
      }
    }
  }

  private void unlock() {
    context.curThread.incrementAndGet();
    synchronized (context.curThread) {
      context.curThread.notifyAll();
    }
  }

  private Object script(boolean callback, String script, Object[] args) {
    return parseScriptResult(Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            List<Object> argList = new ArrayList<Object>();
            if (args != null) {
              argList.addAll(Arrays.asList(args));
            }
            if (callback) {
              argList.add(null);
              node.get().eval("(function(){"
                  + "          this.screenslicerCallback = function(){"
                  + "            this.screenslicerCallbackVal = arguments;"
                  + "          }"
                  + "        }).apply(this);"
                  + "        this.screenslicerJS = function(){"
                  + "          arguments[arguments.length-1] = this.screenslicerCallback;"
                  + "          return (function(){" + script + "}).apply(this, arguments);"
                  + "        };");
            } else {
              node.get().eval("this.screenslicerJS = function(){"
                  + "          return (function(){" + script + "}).apply(this, arguments);"
                  + "        };");
            }
            context.item().httpListener.get().resetStatusCode();
            return node.get().call("screenslicerJS", argList.toArray(new Object[0]));
          }
        }, context.settingsId.get()));
  }

  private Object parseScriptResult(Object obj) {
    if (obj == null || (obj instanceof String && "undefined".equals(obj.toString()))) {
      return null;
    }
    if (obj instanceof Node) {
      try {
        return new ElementServer(new AtomicReference(obj), context);
      } catch (RemoteException e) {
        context.logs.get().exception(e);
        return null;
      }
    }
    if (obj instanceof JSObject) {
      List<Object> result = new ArrayList<Object>();
      for (int i = 0;; i++) {
        Object cur = ((JSObject) obj).getSlot(i);
        if (cur instanceof String && "undefined".equals(cur.toString())) {
          break;
        }
        result.add(parseScriptResult(cur));
      }
      return result;
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

  @Override
  public CoordinatesServer getCoordinates() {
    try {
      return new CoordinatesServer(new org.openqa.selenium.interactions.internal.Coordinates() {

        @Override
        public Point onScreen() {
          return null;
        }

        @Override
        public Point onPage() {
          Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
              new Sync<Object>() {
            @Override
            public Point perform() {
              node.get().call("scrollIntoView");
              return null;
            }
          }, context.settingsId.get());
          return Util.exec(Pause.SHORT, context.statusCode, context.timeouts.get().getScriptTimeoutMS(),
              new Sync<Point>() {
            @Override
            public Point perform() {
              JSObject obj = (JSObject) node.get().call("getBoundingClientRect");
              double y = Double.parseDouble(obj.getMember("top").toString());
              double x = Double.parseDouble(obj.getMember("left").toString());
              y = y < 0d ? 0d : y;
              x = x < 0d ? 0d : x;
              return new Point((int) Math.rint(x) + 1, (int) Math.rint(y) + 1);
            }
          }, context.settingsId.get());
        }

        @Override
        public Point inViewPort() {
          return null;
        }

        @Override
        public Object getAuxiliary() {
          return null;
        }
      });
    } catch (RemoteException e) {
      // TODO 
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public <X> X getScreenshotAs(OutputType<X> arg0) throws WebDriverException {
    context.logs.get().warn("Screenshot not supported on jBrowserDriver WebElements");
    return null;
  }

  @Override
  public byte[] getScreenshot() throws WebDriverException {
    context.logs.get().warn("Screenshot not supported on jBrowserDriver WebElements");
    return null;
  }
}
