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
package com.machinepublishers.jbrowserdriver.diagnostics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;

public class Test {
  private static final int TEST_PORT_HTTP = Integer.parseInt(System.getProperty("jbd.testporthttp", "9000"));
  private static final int TEST_PORT_RMI = Integer.parseInt(System.getProperty("jbd.testportrmi", "10000"));
  private List<String> errors = new ArrayList<String>();
  private int curTest = 0;

  public static void main(String[] args) {
    final long startTime = System.currentTimeMillis();
    Test test = new Test();
    final long endTime = System.currentTimeMillis();
    List<String> errors = test.errors;
    System.out.println("Elapsed Time:  " + (endTime - startTime) + " ms  /  " + test.curTest + " tests");
    if (errors.isEmpty()) {
      System.out.println("System OK.");
    } else {
      for (String error : errors) {
        System.out.println(error);
      }
    }
  }

  public static List<String> run() {
    return new Test().errors;
  }

  private Test() {
    JBrowserDriver driver = null;
    try {
      HttpServer.launch(TEST_PORT_HTTP);
      driver = new JBrowserDriver(
          Settings.builder()
              .portsMax(TEST_PORT_RMI, 1)
              .screen(new Dimension(1024, 768))
              .traceConsole(true)
              .javascriptLog(true)
              .ajaxWait(150)
              .cache(true)
              .ignoreDialogs(false)
              .build());

      /*
       * Load a page
       */
      driver.get("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + TEST_PORT_HTTP);
      test(driver.getStatusCode() == 200);
      long initialRequestId = HttpServer.previousRequestId();

      /*
       * Load page from cache
       */
      driver.get("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + TEST_PORT_HTTP);
      test(driver.getStatusCode() == 200);
      test(HttpServer.previousRequestId() == initialRequestId);

      /*
       * Waiting for AJAX requests
       */
      long waitStart = System.currentTimeMillis();
      driver.pageWait();
      long waitEnd = System.currentTimeMillis();
      test(waitEnd - waitStart > 145 && waitEnd - waitStart < 155);

      /*
       * Javascript logs
       */
      int messages = 0;
      for (LogEntry entry : driver.manage().logs().get("javascript").filter(Level.ALL)) {
        ++messages;
        test(!StringUtils.isEmpty(entry.getMessage()));
      }
      test(messages == 3);

      /*
       * Select DOM elements
       */
      test("test-data-attr".equals(driver.findElement(By.id("divtext1")).getAttribute("data-selected")));
      test(driver.findElement(By.id("divtext1")).getAttribute("undefinedattr") == null);
      test(driver.findElement(By.id("divtext1")).getAttribute("innerText").equals("test1"));
      test(driver.findElements(By.name("divs")).size() == 2);
      test(driver.findElements(By.name("divs")).get(1).getAttribute("innerText").equals("test2"));
      test(driver.findElementByClassName("divclass").getAttribute("id").equals("divtext1"));
      test(driver.findElementsByClassName("divclass").get(1).getAttribute("id").equals("divtext2"));
      test(driver.findElementByCssSelector("#divtext1").getAttribute("id").equals("divtext1"));
      test(driver.findElementsByCssSelector("html > *").get(1).getAttribute("id").equals("testbody"));
      test(driver.findElementById("divtext1").getTagName().equals("div"));
      test(driver.findElementsById("divtext1").get(0).getTagName().equals("div"));
      test(driver.findElementByLinkText("anchor").getAttribute("id").equals("anchor1"));
      test(driver.findElementsByLinkText("anchor").get(1).getAttribute("id").equals("anchor2"));
      test(driver.findElementByName("divs").getAttribute("id").equals("divtext1"));
      test(driver.findElementsByName("divs").get(1).getAttribute("id").equals("divtext2"));
      test(driver.findElementByPartialLinkText("anch").getAttribute("id").equals("anchor1"));
      test(driver.findElementsByPartialLinkText("anch").get(1).getAttribute("id").equals("anchor2"));
      test(driver.findElementByTagName("div").getAttribute("id").equals("divtext1"));
      test(driver.findElementsByTagName("div").get(1).getAttribute("id").equals("divtext2"));
      test(driver.findElementByXPath("//*[@id='divtext1']").getAttribute("id").equals("divtext1"));
      test(driver.findElementByTagName("body").findElement(By.xpath("//*[@id='divtext1']")).getAttribute("id").equals("divtext1"));
      test(driver.findElementsByXPath("//html/*").get(1).getAttribute("id").equals("testbody"));
      test(driver.findElement(By.xpath("//a[contains(@href,'1')]")).getAttribute("id").equals("anchor1"));
      test(driver.findElementsByXPath("//a[contains(@href,'!!!')]").isEmpty());
      test(driver.findElementsByClassName("xx").isEmpty());
      test(driver.findElementByTagName("xx") == null);
      test(driver.findElementByCssSelector("#xx") == null);
      Throwable error = null;
      try {
        driver.findElementsByXPath("!!!");
      } catch (WebDriverException e) {
        error = e;
      }
      test(error != null);
      error = null;
      try {
        driver.findElement(By.id("divtext1")).findElements(By.cssSelector("???"));
      } catch (WebDriverException e) {
        error = e;
      }
      test(error != null);

      /*
       * Typing text
       */
      driver.findElement(By.id("text-input")).sendKeys("testing");
      driver.findElement(By.id("text-input")).sendKeys("");
      test(driver.findElement(By.id("text-input")).getAttribute("value").equals("testing"));
      driver.findElement(By.id("text-input")).sendKeys(JBrowserDriver.KEYBOARD_DELETE);
      test(driver.findElement(By.id("text-input")).getAttribute("value") == null);

      /*
       * Execute javascript
       */
      test(((WebElement) driver.executeScript("return arguments[0];",
          driver.findElement(By.id("divtext1")))).getAttribute("innerText").equals("test1"));
      test(((WebElement) driver.executeScript("return arguments[0].parentNode;",
          driver.findElement(By.id("divtext1")))).getTagName().equals("body"));
      test(((WebElement) ((JavascriptExecutor) driver.findElement(By.id("divtext1"))).executeAsyncScript("arguments[0](this);"))
          .getAttribute("innerText").equals("test1"));
      test((driver.executeAsyncScript("arguments[1](arguments[0].innerText);",
          driver.findElement(By.id("divtext1")))).equals("test1"));
      Map<String, Object> map = (Map<String, Object>) driver
          .executeScript(
              "return {"
                  + "key1:['value1','value2','value3'], "
                  + "key2:5,"
                  + "key3:function(){return 'testing';}, "
                  + "key4:undefined, key5:null, key6:1/0, key7:0/0, key8:'', key9:[], key10:{}, key11:[{},{}],"
                  + "key12:document.getElementById('divtext1'), "
                  + "key13:document.getElementsByName('divs'), "
                  + "key14:[document.getElementById('divtext1'),document.getElementsByName('divs'),{subkey1:'subval1'}]};");
      test(map.size() == 14);
      test(((List) map.get("key1")).size() == 3);
      test(((List) map.get("key1")).get(2).equals("value3"));
      test(((List) map.get("key1")).get(2) instanceof String);
      test(map.get("key2").equals(new Long(5)));
      test("function () {return 'testing';}".equals(map.get("key3")));
      test(map.get("key4") == null);
      test(map.get("key5") == null);
      test(Double.isInfinite(((Double) map.get("key6")).doubleValue()));
      test(Double.isNaN(((Double) map.get("key7")).doubleValue()));
      test("".equals(map.get("key8")));
      test(map.get("key9") instanceof List);
      test(map.get("key10") instanceof Map);
      test(((List) map.get("key11")).size() == 2);
      test(((Map) ((List) map.get("key11")).get(1)).isEmpty());
      test("test1".equals(((WebElement) map.get("key12")).getAttribute("innerText")));
      test(((List<WebElement>) map.get("key13")).size() == 2);
      test(((List<WebElement>) map.get("key13")).get(1).getAttribute("innerText").equals("test2"));
      test(((List) map.get("key14")).size() == 3);
      test(((List) ((List) map.get("key14")).get(1)).size() == 2);
      test(((WebElement) ((List) ((List) map.get("key14")).get(1)).get(1)).getAttribute("innerText").equals("test2"));
      test(((Map) ((List) map.get("key14")).get(2)).size() == 1);
      test("subval1".equals(((Map) ((List) map.get("key14")).get(2)).get("subkey1")));
      test(((List) driver.executeScript("return [];")).isEmpty());
      test(((Map) driver.executeScript("return {};")).isEmpty());
      error = null;
      try {
        driver.executeScript("invalid.execute()");
      } catch (WebDriverException e) {
        error = e;
      }
      test(error != null);

      /*
       * DOM element properties
       */
      WebElement element = driver.findElement(By.id("divtext1"));
      Point point = element.getLocation();
      test(point.getX() > 0);
      test(point.getY() > 0);
      Dimension dimension = element.getSize();
      test(dimension.width > 0);
      test(dimension.height > 0);
      Rectangle rect = element.getRect();
      test(rect.x == point.getX());
      test(rect.y == point.getY());
      test(rect.width == dimension.getWidth());
      test(rect.height == dimension.getHeight());

      /*
       * Cookie manager
       */
      driver.manage().addCookie(new Cookie("testname", "testvalue"));
      Cookie cookie = driver.manage().getCookieNamed("testname");
      test(cookie.getValue().equals("testvalue"));
      test(InetAddress.getLoopbackAddress().getHostAddress().equals(cookie.getDomain()));

      /*
       * Screenshots
       */
      test(driver.getScreenshotAs(OutputType.BYTES).length > 0);

      /*
       * File Input Type
       */
      driver.findElement(By.id("upload")).sendKeys("some-file");
      test("event-test".equals(driver.findElement(By.id("file-input-onchange")).getText()));
      test(driver.findElement(By.id("upload")).getAttribute("value").endsWith("some-file"));

      /*
       * Javascript alerts
       */
      driver.findElement(By.tagName("button")).click();
      test(driver.switchTo().alert().getText().equals("test-alert"));
      driver.switchTo().alert().dismiss();
      test(driver.switchTo().alert().getText().equals("test-confirm"));
      driver.switchTo().alert().dismiss();
      test(driver.switchTo().alert().getText().equals("test-prompt"));
      driver.switchTo().alert().sendKeys("test-input");
      driver.switchTo().alert().accept();
      test(driver.findElement(By.id("testspan")).getAttribute("innerHTML").equals("test-input"));

      /*
       * Request headers
       */
      List<String> request = HttpServer.previousRequest();
      if (TEST_PORT_HTTP != 443 && TEST_PORT_HTTP != 80) {
        test(request.get(1).endsWith(":" + TEST_PORT_HTTP));
      }
      test(request.size() > 1);
      Set<String> headers = new HashSet<String>();
      for (String line : request) {
        if (line.contains(":")) {
          headers.add(line.split(":")[0].toLowerCase());
        }
      }
      test(request.size() - 2 == headers.size());

      /*
       * Frames
       */
      driver.switchTo().frame(driver.findElementByTagName("iframe"));
      test(driver.findElementById("iframebody") != null);
      driver.switchTo().parentFrame();
      test(driver.findElementById("testbody") != null);
      driver.switchTo().frame(0);
      test(driver.findElementById("iframebody") != null);
      driver.switchTo().defaultContent();
      driver.switchTo().frame("testiframe");
      test(driver.findElementById("iframebody") != null);
      driver.get("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + TEST_PORT_HTTP);
      test(driver.getPageSource() != null);
      driver.switchTo().frame(driver.findElementByTagName("iframe"));
      test(driver.findElementById("iframebody") != null);
      driver.switchTo().parentFrame();
      driver.findElement(By.id("anchor3")).click();
      test(driver.getPageSource() != null);
      driver.switchTo().frame(driver.findElementByTagName("iframe"));
      driver.findElement(By.id("iframe-anchor")).click(); //TODO iframe coord offset needed on other methods too besides click() ?
      driver.pageWait();
      test(HttpServer.previousRequest().get(0).startsWith("GET /iframe.htm?param=fromiframe"));
      //TODO fingerprinting
      //System.out.println(driver.findElement(By.id("iframe-useragent")).getAttribute("innerHTML"));
      //System.out.println(driver.findElement(By.id("iframe-nested-useragent")).getAttribute("innerHTML"));

      /*
       * Redirects and cookies
       */
      driver.get("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + TEST_PORT_HTTP + "/redirect/site1");
      test(HttpServer.previousRequestId() != initialRequestId);
      test(driver.getStatusCode() == 200);
      test(driver.getCurrentUrl().endsWith("/redirect/site2"));
      cookie = driver.manage().getCookieNamed("JSESSIONID");
      test(cookie.getValue().equals("ABC123"));
      test(InetAddress.getLoopbackAddress().getHostAddress().equals(cookie.getDomain()));

      /*
       * Cookies set by Javascript
       */
      test("jsCookieValue1".equals(driver.manage().getCookieNamed("jsCookieName1").getValue()));
      test("jsCookieValue2".equals(driver.manage().getCookieNamed("jsCookieName2").getValue()));
      test("jsCookieValue3".equals(driver.manage().getCookieNamed("jsCookieName3").getValue()));
      test("jsCookieValue4".equals(driver.manage().getCookieNamed("jsCookieName4").getValue()));

      /*
       * Window size and position
       */
      driver.manage().window().setSize(new Dimension(5000, 5000));
      test(driver.manage().window().getSize().getWidth() == 1024);
      test(driver.manage().window().getSize().getHeight() == 768);
      driver.manage().window().setSize(new Dimension(800, 600));
      test(driver.manage().window().getSize().getWidth() == 800);
      test(driver.manage().window().getSize().getHeight() == 600);
      driver.manage().window().setPosition(new Point(5000, 5000));
      test(driver.manage().window().getPosition().getX() == 224);
      test(driver.manage().window().getPosition().getY() == 168);
      driver.manage().window().setPosition(new Point(20, 50));
      test(driver.manage().window().getPosition().getX() == 20);
      test(driver.manage().window().getPosition().getY() == 50);
      driver.manage().window().maximize();
      test(driver.manage().window().getPosition().getX() == 0);
      test(driver.manage().window().getPosition().getY() == 0);
      test(driver.manage().window().getSize().getWidth() == 1024);
      test(driver.manage().window().getSize().getHeight() == 768);
      driver.manage().window().setSize(new Dimension(800, 600));
      driver.manage().window().setPosition(new Point(20, 50));
      driver.manage().window().fullscreen();
      test(driver.manage().window().getPosition().getX() == 0);
      test(driver.manage().window().getPosition().getY() == 0);
      test(driver.manage().window().getSize().getWidth() == 1024);
      test(driver.manage().window().getSize().getHeight() == 768);
      driver.manage().window().fullscreen();
      test(driver.manage().window().getPosition().getX() == 20);
      test(driver.manage().window().getPosition().getY() == 50);
      test(driver.manage().window().getSize().getWidth() == 800);
      test(driver.manage().window().getSize().getHeight() == 600);
      driver.manage().window().setSize(new Dimension(400, 200));
      driver.manage().window().setPosition(new Point(10, 30));
      test(driver.manage().window().getPosition().getX() == 10);
      test(driver.manage().window().getPosition().getY() == 30);
      test(driver.manage().window().getSize().getWidth() == 400);
      test(driver.manage().window().getSize().getHeight() == 200);
      driver.manage().window().setSize(new Dimension(1024, 768));
      test(driver.manage().window().getPosition().getX() == 0);
      test(driver.manage().window().getPosition().getY() == 0);
      test(driver.manage().window().getSize().getWidth() == 1024);
      test(driver.manage().window().getSize().getHeight() == 768);

      /*
       * Element visibility
       */
      test(driver.findElement(By.id("iframe-anchor")).isDisplayed());
      test(!driver.findElement(By.id("anchor-visibility-hidden")).isDisplayed());
      test(!driver.findElement(By.id("anchor-display-none")).isDisplayed());
      error = null;
      try {
        driver.findElement(By.id("anchor-visibility-hidden")).click();
      } catch (ElementNotVisibleException e) {
        error = e;
      }
      test(error != null);
      error = null;
      try {
        driver.findElement(By.id("anchor-display-none")).click();
      } catch (ElementNotVisibleException e) {
        error = e;
      }
      test(error != null);

      /*
       * Operations on elements that no longer exist
       */
      WebElement body = driver.findElement(By.tagName("body"));
      test(!StringUtils.isEmpty(body.getAttribute("outerHTML")));
      driver.get("about:blank");
      error = null;
      try {
        body.getAttribute("outerHTML");
      } catch (StaleElementReferenceException e) {
        error = e;
      }
      test(error != null);

      /*
       * Timeouts
       */
      driver.manage().timeouts().pageLoadTimeout(1, TimeUnit.MILLISECONDS);
      error = null;
      try {
        driver.get("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + TEST_PORT_HTTP + "/wait-forever");
      } catch (TimeoutException e) {
        error = e;
      }
      test(error != null);

    } catch (Throwable t) {
      errors.add(failureLabel(curTest + 1, t));
    } finally {
      try {
        driver.quit();
        HttpServer.stop();
      } catch (Throwable t) {
        errors.add(toString(t));
      }
    }
  }

  private static String failureLabel(int curTest, Throwable throwable) {
    String stackTrace = throwable == null ? "" : " -- " + toString(throwable);

    StackTraceElement[] elements = throwable == null ? new Throwable().getStackTrace() : throwable.getStackTrace();
    String testLocation = "";
    for (int i = 0; i < elements.length; i++) {
      if (Test.class.getName().equals(elements[i].getClassName())
          && "<init>".equals(elements[i].getMethodName())) {
        testLocation = elements[i].toString();
      }
    }

    return "Test #" + curTest + " failed -- " + testLocation + stackTrace;
  }

  private void test(boolean bool) {
    ++curTest;
    if (!bool) {
      errors.add(failureLabel(curTest, null));
    }
  }

  private static String toString(Throwable t) {
    StringWriter writer = new StringWriter();
    t.printStackTrace(new PrintWriter(writer));
    return writer.toString().replace("\n", " ").replace("\t", " ");
  }
}
