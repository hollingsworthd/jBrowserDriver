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

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class Test {
  private static final int TEST_PORT = Integer.parseInt(System.getProperty("jbd.testport", "9000"));

  public static List<String> run() {
    List<String> errors = new ArrayList<String>();
    JBrowserDriver driver = null;
    try {
      HttpServer.launch(TEST_PORT);
      driver = new JBrowserDriver();
      driver.get("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + TEST_PORT);
      if (driver.getStatusCode() != 200) {
        errors.add("Status code not 200");
      }
      if (!driver.findElement(By.id("divtext")).getAttribute("innerText").equals("test1")) {
        errors.add("Finding div by ID and getting innerText failed");
      }
      if (driver.findElements(By.name("divs")).size() != 2) {
        errors.add("Could not find elements by name");
      }
      if (!driver.findElements(By.name("divs")).get(1).getAttribute("innerText").equals("test2")) {
        errors.add("Finding div by name and getting innerText failed");
      }
    } catch (Throwable t) {
      errors.add(toString(t));
    } finally {
      try {
        driver.quit();
        HttpServer.stop();
      } catch (Throwable t) {
        errors.add(toString(t));
      }
    }
    return errors;
  }

  private static String toString(Throwable t) {
    StringWriter writer = new StringWriter();
    t.printStackTrace(new PrintWriter(writer));
    return "Runtime exception: " + writer.toString().replaceAll("\n", " ");
  }

  public static void main(String[] args) {
    List<String> errors = JBrowserDriver.test();
    System.out.println("Tests Passed: " + errors.isEmpty());
    for (String error : errors) {
      System.out.println("    " + error);
    }
  }
}
