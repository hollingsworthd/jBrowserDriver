/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.web.WebEngine;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import netscape.javascript.JSObject;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
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
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;

import com.machinepublishers.jbrowserdriver.Robot.MouseButton;
import com.machinepublishers.jbrowserdriver.Util.Sync;

public class Element implements WebElement, JavascriptExecutor, FindsById, FindsByClassName,
    FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName, FindsByXPath {
  private static final AtomicLong latestThread = new AtomicLong();
  private static final AtomicLong curThread = new AtomicLong();
  private static final Pattern rgb = Pattern.compile(
      "rgb\\(([0-9]{1,3}), ([0-9]{1,3}), ([0-9]{1,3})\\)");
  private final AtomicReference<Node> node;
  private final AtomicReference<Robot> robot;
  private final AtomicReference<Timeouts> timeouts;
  private final boolean isWindow;

  Element(final AtomicReference<Node> node, final AtomicReference<Robot> robot,
      final AtomicReference<Timeouts> timeouts) {
    this.isWindow = node.get() instanceof Document;
    this.node = node;
    this.robot = robot;
    this.timeouts = timeouts;
  }

  static Element create(final AtomicReference<WebEngine> engine, final AtomicReference<Robot> robot,
      final AtomicReference<Timeouts> timeouts) {
    final AtomicReference<Node> doc = new AtomicReference<Node>(
        Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Document>() {
          @Override
          public Document perform() {
            return engine.get().getDocument();
          }
        }));
    return new Element(doc, robot, timeouts);
  }

  @Override
  public void click() {
    Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Object>() {
      @Override
      public Object perform() {
        ((JSObject) node.get()).call("scrollIntoView");
        JSObject obj = (JSObject) ((JSObject) node.get()).call("getBoundingClientRect");
        double y = Double.parseDouble(obj.getMember("top").toString());
        double x = Double.parseDouble(obj.getMember("left").toString());
        robot.get().mouseMove(x, y);
        robot.get().mouseClick(MouseButton.LEFT);
        return null;
      }
    });
  }

  @Override
  public void submit() {
    Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Object>() {
      @Override
      public Object perform() {
        if (node.get() instanceof HTMLInputElement) {
          ((HTMLInputElement) node.get()).getForm().submit();
        } else if (node.get() instanceof HTMLFormElement) {
          ((HTMLFormElement) node.get()).submit();
        }
        return null;
      }
    });
  }

  @Override
  public void sendKeys(final CharSequence... keys) {
    Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Object>() {
      @Override
      public Object perform() {
        ((JSObject) node.get()).call("scrollIntoView");
        ((JSObject) node.get()).call("focus");
        robot.get().keysType(keys);
        return null;
      }
    });
  }

  @Override
  public void clear() {
    Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Object>() {
      @Override
      public Object perform() {
        ((JSObject) node.get()).call("scrollIntoView");
        ((JSObject) node.get()).call("focus");
        ((JSObject) node.get()).call("setValue", "");
        return null;
      }
    });
  }

  @Override
  public String getAttribute(final String attrName) {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<String>() {
      @Override
      public String perform() {
        String val = (String) ((JSObject) node.get()).getMember(attrName);
        return val == null || val.equals("undefined") ? "" : val;
      }
    });
  }

  @Override
  public String getCssValue(final String name) {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<String>() {
      @Override
      public String perform() {
        return cleanUpCssVal((String) ((JSObject) node.get()).eval("var me = this;"
            + "(function(){"
            + "  return window.getComputedStyle(me).getPropertyValue('" + name + "');"
            + "})();"));
      }
    });
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
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Point>() {
      @Override
      public Point perform() {
        JSObject obj = (JSObject) ((JSObject) node.get()).call("getBoundingClientRect");
        int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
        int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
        return new Point(x, y);
      }
    });
  }

  @Override
  public Dimension getSize() {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Dimension>() {
      @Override
      public Dimension perform() {
        JSObject obj = (JSObject) ((JSObject) node.get()).call("getBoundingClientRect");
        int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
        int y2 = (int) Math.rint(Double.parseDouble(obj.getMember("bottom").toString()));
        int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
        int x2 = (int) Math.rint(Double.parseDouble(obj.getMember("right").toString()));
        return new Dimension(x2 - x, y2 - y);
      }
    });
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
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Boolean>() {
      @Override
      public Boolean perform() {
        JSObject obj = (JSObject) ((JSObject) node.get()).call("getBoundingClientRect");
        int y = (int) Math.rint(Double.parseDouble(obj.getMember("top").toString()));
        int y2 = (int) Math.rint(Double.parseDouble(obj.getMember("bottom").toString()));
        int x = (int) Math.rint(Double.parseDouble(obj.getMember("left").toString()));
        int x2 = (int) Math.rint(Double.parseDouble(obj.getMember("right").toString()));
        return (Boolean)
        ((JSObject) node.get()).eval("var me = this;"
            + "        (function(){"
            + "          for(var i = " + x + "; i < " + (x2 + 1) + "; i++){"
            + "            for(var j = " + y + "; j < " + (y2 + 1) + "; j++){"
            + "              if(document.elementFromPoint(i,j) == me){"
            + "                return true;"
            + "              }"
            + "            }"
            + "          }"
            + "          return false;})();");
      }
    });
  }

  @Override
  public boolean isEnabled() {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Boolean>() {
      @Override
      public Boolean perform() {
        String val = ((JSObject) node.get()).getMember("disabled").toString();
        return val == null || "undefined".equals(val) || val.isEmpty();
      }
    });
  }

  @Override
  public boolean isSelected() {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Boolean>() {
      @Override
      public Boolean perform() {
        String selected = ((JSObject) node.get()).getMember("selected").toString();
        String checked = ((JSObject) node.get()).getMember("checked").toString();
        return (selected != null && !"undefined".equals(selected) && !selected.isEmpty())
            || (checked != null && !"undefined".equals(checked) && !checked.isEmpty());
      }
    });
  }

  @Override
  public WebElement findElement(By by) {
    return by.findElement(this);
  }

  @Override
  public List<WebElement> findElements(By by) {
    return by.findElements(this);
  }

  @Override
  public WebElement findElementByXPath(final String expr) {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<WebElement>() {
      @Override
      public WebElement perform() {
        try {
          final XPath xPath = XPathFactory.newInstance().newXPath();
          return new Element(new AtomicReference<Node>((Node) xPath.evaluate(
              expr, node.get(), XPathConstants.NODE)), robot, timeouts);
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Override
  public List<WebElement> findElementsByXPath(final String expr) {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<List<WebElement>>() {
      @Override
      public List<WebElement> perform() {
        try {
          XPath xPath = XPathFactory.newInstance().newXPath();
          NodeList list = (NodeList) xPath.evaluate(expr, node.get(), XPathConstants.NODESET);
          List<WebElement> elements = new ArrayList<WebElement>();
          for (int i = 0; i < list.getLength(); i++) {
            elements.add(new Element(new AtomicReference<Node>(list.item(i)), robot, timeouts));
          }
          return elements;
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    List<WebElement> list = byTagName(tagName);
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    return byTagName(tagName);
  }

  private List<WebElement> byTagName(final String tagName) {
    return (List<WebElement>) executeScript("return this.getElementsByTagName('" + tagName + "');");
  }

  @Override
  public WebElement findElementByCssSelector(final String expr) {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<WebElement>() {
      @Override
      public WebElement perform() {
        JSObject result = (JSObject) ((JSObject) node.get()).call("querySelector", expr);
        if (result == null) {
          return null;
        }
        return new Element(new AtomicReference<Node>((Node) result), robot, timeouts);
      }
    });
  }

  @Override
  public List<WebElement> findElementsByCssSelector(final String expr) {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<List<WebElement>>() {
      @Override
      public List<WebElement> perform() {
        List<WebElement> elements = new ArrayList<WebElement>();
        JSObject result = (JSObject) ((JSObject) node.get()).call("querySelectorAll", expr);
        for (int i = 0;; i++) {
          Object cur = result.getSlot(i);
          if (cur instanceof Node) {
            elements.add(new Element(new AtomicReference<Node>((Node) cur), robot, timeouts));
          } else {
            break;
          }
        }
        return elements;
      }
    });
  }

  @Override
  public WebElement findElementByName(String name) {
    return findElementByCssSelector("*[name='" + name + "']");
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    return findElementsByCssSelector("*[name='" + name + "']");
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    List<WebElement> list = byLinkText(text, false, false);
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    List<WebElement> list = byLinkText(text, false, true);
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    return byLinkText(text, true, false);
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    return byLinkText(text, true, true);
  }

  private List<WebElement> byLinkText(final String text,
      final boolean multiple, final boolean partial) {
    return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<List<WebElement>>() {
      @Override
      public List<WebElement> perform() {
        List<WebElement> nodes = (List<WebElement>) findElementsByTagName("a");
        List<WebElement> elements = new ArrayList<WebElement>();
        for (WebElement cur : nodes) {
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

  @Override
  public WebElement findElementByClassName(String cssClass) {
    List<WebElement> list = byCssClass(cssClass);
    return list.isEmpty() ? null : list.get(0);
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    return byCssClass(cssClass);
  }

  private List<WebElement> byCssClass(String cssClass) {
    return (List<WebElement>) executeScript("return this.getElementsByClassName('" + cssClass + "');");
  }

  @Override
  public WebElement findElementById(final String id) {
    return findElementByCssSelector("*[id='" + id + "']");
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    return findElementsByCssSelector("*[id='" + id + "']");
  }

  @Override
  public Object executeAsyncScript(final String script, final Object... args) {
    lock();
    try {
      Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Object>() {
        @Override
        public Object perform() {
          return script(true, script, args);
        }
      });
      int sleep = 1;
      final int sleepBackoff = 2;
      final int sleepMax = 500;
      while (true) {
        sleep = sleep < sleepMax ? sleep * sleepBackoff : sleep;
        try {
          Thread.sleep(sleep);
        } catch (InterruptedException e) {}
        Object result = Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Object>() {
          @Override
          public Object perform() {
            return ((JSObject) node.get()).eval("(function(){return this.screenslicerCallbackVal;})();");
          }
        });
        if (!(result instanceof String) || !"undefined".equals(result)) {
          result = parseScriptResult(result);
          if (result instanceof List) {
            if (((List) result).size() == 0) {
              return null;
            }
            if (((List) result).size() == 1) {
              return ((List) result).get(0);
            }
          }
          return result;
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
      return Util.exec(timeouts.get().getScriptTimeoutMS(), new Sync<Object>() {
        @Override
        public Object perform() {
          return script(false, script, args);
        }
      });
    } finally {
      unlock();
    }
  }

  private void lock() {
    long myThread = latestThread.incrementAndGet();
    synchronized (curThread) {
      while (myThread != curThread.get() + 1) {
        try {
          curThread.wait();
        } catch (Exception e) {
          Logs.exception(e);
        }
      }
    }
  }

  private void unlock() {
    curThread.incrementAndGet();
    synchronized (curThread) {
      curThread.notifyAll();
    }
  }

  private Object script(boolean callback, String script, Object[] args) {
    args = args == null ? new Object[0] : args;
    if (callback) {
      Object[] tmp = new Object[args.length + 1];
      for (int i = 0; i < args.length; i++) {
        tmp[i] = args[i];
      }
      args = tmp;
      ((JSObject) this.node.get()).eval("(function(){"
          + "          this.screenslicerCallback = function(){"
          + "            this.screenslicerCallbackVal = arguments;"
          + "          }"
          + "        }).apply(this);"
          + "        this.screenslicerJS = function(){"
          + (isWindow ? "var window = this;" : "")
          + "          arguments[arguments.length-1] = this.screenslicerCallback;"
          + "          return (function(){" + script + "}).apply(this, arguments);"
          + "        };");
    } else {
      ((JSObject) this.node.get()).eval("this.screenslicerJS = function(){"
          + (isWindow ? "var window = this;" : "")
          + "          return (function(){" + script + "}).apply(this, arguments);"
          + "        };");
    }
    return parseScriptResult(((JSObject) this.node.get()).call("screenslicerJS", args));
  }

  private Object parseScriptResult(Object obj) {
    if (obj == null || (obj instanceof String && "undefined".equals(obj))) {
      return null;
    }
    if (obj instanceof Node) {
      return new Element(new AtomicReference<Node>((Node) obj), robot, timeouts);
    }
    if (obj instanceof JSObject) {
      JSObject jsObj = (JSObject) obj;
      List<Object> result = new ArrayList<Object>();
      for (int i = 0;; i++) {
        Object cur = jsObj.getSlot(i);
        if (cur instanceof String && "undefined".equals(cur)) {
          break;
        }
        result.add(parseScriptResult(cur));
      }
      return result;
    }
    if (obj instanceof Boolean) {
      return obj;
    }
    if (obj instanceof Long) {
      return obj;
    }
    if (obj instanceof Integer) {
      return new Long((Integer) obj);
    }
    if (obj instanceof Double) {
      return obj;
    }
    if (obj instanceof Float) {
      return new Double((Float) obj);
    }
    return obj.toString();
  }

}
