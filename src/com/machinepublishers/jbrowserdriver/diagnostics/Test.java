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
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class Test {
  private static final int TEST_PORT = Integer.parseInt(System.getProperty("jbd.testport", "9000"));
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
      HttpServer.launch(TEST_PORT);
      driver = new JBrowserDriver();
      driver.get("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + TEST_PORT);

      test(driver.getStatusCode() == 200);

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

      driver.manage().addCookie(new Cookie("testname", "testvalue"));
      test(driver.manage().getCookieNamed("testname").getValue().equals("testvalue"));

      test(driver.getScreenshotAs(OutputType.BYTES).length > 0);

      driver.findElement(By.tagName("button")).click();
      test(driver.switchTo().alert().getText().equals("test-alert"));
      driver.switchTo().alert().dismiss();
      test(driver.switchTo().alert().getText().equals("test-confirm"));
      driver.switchTo().alert().dismiss();
      test(driver.switchTo().alert().getText().equals("test-prompt"));
      driver.switchTo().alert().sendKeys("test-input");
      driver.switchTo().alert().accept();
      test(driver.findElement(By.id("testspan")).getAttribute("innerHTML").equals("test-input"));
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
