package com.machinepublishers.jbrowserdriver.diagnostics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class Test {
  public static List<String> run() {
    List<String> errors = new ArrayList<String>();
    try {
      HttpServer.launch();
      JBrowserDriver driver = new JBrowserDriver();
      driver.get("http://127.0.0.1:9000");
      if (driver.getStatusCode() != 200) {
        errors.add("Status code not 200");
      }
      driver.quit();
      HttpServer.stop();
    } catch (Throwable t) {
      StringWriter writer = new StringWriter();
      t.printStackTrace(new PrintWriter(writer));
      errors.add("Runtime exception: " + writer.toString().replaceAll("\n", " "));
    }
    return errors;
  }

  public static void main(String[] args) {
    List<String> errors = JBrowserDriver.test();
    System.out.println("Tests Passed: " + errors.isEmpty());
    for (String error : errors) {
      System.out.println("    " + error);
    }
  }
}
