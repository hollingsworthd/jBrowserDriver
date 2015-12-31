package com.machinepublishers.jbrowserdriver.diagnostics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class Test {
  public static List<String> run() {
    List<String> errors = new ArrayList<String>();
    JBrowserDriver driver = null;
    try {
      HttpServer.launch();
      driver = new JBrowserDriver();
      driver.get("http://127.0.0.1:9000");
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
