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
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;

public class Test {
  private static final int TEST_PORT_HTTP = Integer.parseInt(System.getProperty("jbd.testporthttp", "9000"));
  private static final int TEST_PORT_RMI = Integer.parseInt(System.getProperty("jbd.testportrmi", "10000"));
  private List<String> errors = new ArrayList<String>();
  private int curTest = 0;

  public static void main(String[] args) {
    List<String> errors = new Test().errors;
    System.out.println("Tests Passed: " + errors.isEmpty());
    for (String error : errors) {
      System.out.println("    " + error);
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
          Settings.builder().portsMax(TEST_PORT_RMI, 1).traceConsole(true).cache(true).ignoreDialogs(false).build());

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
       * Select DOM elements
       */
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

      /*
       * Cookie manager
       */
      driver.manage().addCookie(new Cookie("testname", "testvalue"));
      test(driver.manage().getCookieNamed("testname").getValue().equals("testvalue"));

      /*
       * Screenshots
       */
      test(driver.getScreenshotAs(OutputType.BYTES).length > 0);

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

      /*
       * Redirects and cookies
       */
      driver.get("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + TEST_PORT_HTTP + "/redirect/site1");
      test(HttpServer.previousRequestId() != initialRequestId);
      test(driver.getStatusCode() == 200);
      test(driver.getCurrentUrl().endsWith("/redirect/site2"));
      test(driver.manage().getCookieNamed("JSESSIONID").getValue().equals("ABC123"));

      /*
       * Cookies set by Javascript
       */
      test("jsCookieValue1".equals(driver.manage().getCookieNamed("jsCookieName1").getValue()));
      test("jsCookieValue2".equals(driver.manage().getCookieNamed("jsCookieName2").getValue()));
      test("jsCookieValue3".equals(driver.manage().getCookieNamed("jsCookieName3").getValue()));
      test("jsCookieValue4".equals(driver.manage().getCookieNamed("jsCookieName4").getValue()));

    } catch (Throwable t) {
      errors.add("Test #" + (curTest + 1) + " -- " + toString(t));
    } finally {
      try {
        driver.quit();
        HttpServer.stop();
      } catch (Throwable t) {
        errors.add(toString(t));
      }
    }
  }

  private void test(boolean bool) {
    ++curTest;
    if (!bool) {
      errors.add("Test #" + curTest + " -- " + toString(new Throwable()));
    }
  }

  private static String toString(Throwable t) {
    StringWriter writer = new StringWriter();
    t.printStackTrace(new PrintWriter(writer));
    return "Runtime exception: " + writer.toString().replaceAll("\n", " ");
  }
}
