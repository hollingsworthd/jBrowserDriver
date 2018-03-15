package com.machinepublishers;

import com.google.common.base.Stopwatch;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import com.machinepublishers.testserver.PortUtil;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

public class AjaxResponseTest {
  @Test
  public void testAjaxSpeed() throws Exception {

    int mainPort = PortUtil.findRandomOpenPortOnAllLocalInterfaces();
    final Server server = new Server(mainPort);

    Thread httpServerThread = new Thread(() -> {
      try {
        server.setHandler(new AbstractHandler() {
          @Override
          public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse
              response) throws IOException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            System.out.println(target + " is the request");
            if (target.contains("infinite")) {
              // page will infinite hard cpu javascript loop
              response.getWriter().println("<!DOCTYPE html>\n" +
                  "<html>\n" +
                  "<body>\n" +
                  "<div id=\"container\">hard javascript loop</div>\n" +
                  "<script>\n" +
                  "    while (true) {}\n" +
                  "  </script>\n" +
                  "</body>\n" +
                  "</html>\n");
            } else if (target.toLowerCase().contains("pagetimeout")) {
              try {
                Thread.sleep(1000000);
                response.getWriter().println("should time out");
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
              }
            } else if (target.contains("page")) {
              // vary between xhtml manual ajax and jquery ajax
              if (new Random().nextBoolean()) {
                response.getWriter().println("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script>\n" +
                    "<script>\nvar nowMs = Date.now();\n" +
                    "$(function() {\n" +
                    "$.get(\"ajax1\").done(function() {\n" +
                    "  $('#demo1').html('done loading 1 time taken ms = [' + (Date.now() - nowMs) + ']');\n" +
                    "});" +
                    "$.get(\"ajax2\").done(function() {\n" +
                    "  $('#demo2').html('done loading 2 time taken ms = [' + (Date.now() - nowMs) + ']');\n" +
                    "});" +
                    "$.get(\"ajax3\").done(function() {\n" +
                    "  $('#demo3').html('done loading 3 time taken ms = [' + (Date.now() - nowMs) + ']');\n" +
                    "});" +
                    "});" +
                    "</script>\n" +
                    "<body>\n" +
                    "\n" +
                    "<div id=\"demo1\">\n" +
                    "  <h2>still waiting for reload 1</h2>\n" +
                    "</div>\n" +
                    "<div id=\"demo2\">\n" +
                    "  <h2>still waiting for reload 2</h2>\n" +
                    "</div>\n" +
                    "<div id=\"demo3\">\n" +
                    "  <h2>still waiting for reload 3</h2>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>");
              } else {
                response.getWriter().println("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<script>\nvar nowMs = Date.now();\n" +
                    "function loadDoc(idx) {\n" +
                    "  var xhttp = new XMLHttpRequest();\n" +
                    "  var xhttp = new XMLHttpRequest();\n" +
                    "  xhttp.onreadystatechange = function() {\n" +
                    "    if (this.readyState == 4 && this.status == 200) {\n" +
                    "     document.getElementById(\"demo\" + idx).innerHTML = this.responseText + ' time taken ms = ['" +
                    " + (Date.now() - nowMs) + ']';\n" +
                    "    }\n" +
                    "  };\n" +
                    "  xhttp.open(\"GET\", \"ajaxCall\" + idx, true);\n" +
                    "  xhttp.send();\n" +
                    "}\n" +
                    "function doLoads() {\n" +
                    "  setTimeout(function () { loadDoc(1) }, 100);\n" +
                    "  setTimeout(function () { loadDoc(2) }, 100);\n" +
                    "  setTimeout(function () { loadDoc(3) }, 100);\n" +
                    "}\n" +
                    "</script>\n" +
                    "<body onload=\"doLoads()\">\n" +
                    "<div>\n" +
                    "  <div id=\"demo1\">still waiting for reload 1</div>\n" +
                    "  <div id=\"demo2\">still waiting for reload 2</div>\n" +
                    "  <div id=\"demo3\">still waiting for reload 3</div>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>");
              }
            } else if (target.contains("ajaxtimeout")) {
              // vary between xhtml manual ajax and jquery ajax
              if (new Random().nextBoolean()) {
                response.getWriter().println("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script>\n" +
                    "<script>\n" +
                    "$(function() {\n" +
                    "$.get(\"pagetimeout\").done(function() {\n" +
                    "  $('#demo2').html('done loading 2');\n" +
                    "});" +
                    "});" +
                    "</script>\n" +
                    "<body>\n" +
                    "\n" +
                    "<div id=\"demo2\">\n" +
                    "  <h2>still waiting for reload 2</h2>\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>");
              } else {
                response.getWriter().println("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<script>\n" +
                    "function loadDoc(idx) {\n" +
                    "  var xhttp = new XMLHttpRequest();\n" +
                    "  xhttp.onreadystatechange = function() {\n" +
                    "    if (this.readyState == 4 && this.status == 200) {\n" +
                    "     document.getElementById(\"demo\" + idx).innerHTML = this.responseText;\n" +
                    "    }\n" +
                    "  };\n" +
                    "  xhttp.open(\"GET\", \"pagetimeout\" + idx, true);\n" +
                    "  xhttp.send();\n" +
                    "}\n" +
                    "function doLoads() {\n" +
                    "  setTimeout(function () { loadDoc(1) }, 100);\n" +
                    "}\n" +
                    "</script>\n" +
                    "<body onload=\"doLoads()\">\n" +
                    "<div>\n" +
                    "  <div id=\"demo1\">still waiting for reload 1</div>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>");
              }
            } else if (target.toLowerCase().contains("ajax")) {
              int typeOfDelay = new Random().nextInt(10);
              int jsDelay;
              if (typeOfDelay >= 8) { // long wait
                jsDelay = new Random().nextInt(20);
              } else if (typeOfDelay >= 6) { // medium wait
                jsDelay = new Random().nextInt(10);
              } else {
                jsDelay = new Random().nextInt(3); // small wait
              }
              try {
                Thread.sleep(jsDelay * 1000);
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
              }
              response.getWriter().println(" successfully loaded ajax call.");
            } else { // index.html
              response.getWriter().println("<html>\n" +
                  "<body>");
              if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                  response.getWriter().println("<h1>Cookie: " + c.getName() + " = " + c.getValue() + "</h1>");
                }
              }
              for (int i = 0; i < 10000; ++i) {
                int pageType = new Random().nextInt(40);
                if (pageType <= 36) {
                  response.getWriter().println("<p><a href=\"page" + i + ".html\">page " + i + "</a></p>");
                } else if (pageType <= 37) {
                  response.getWriter().println("<p><a href=\"page" + i + ".html\">pagetimeout " + i + "</a></p>");
                } else if (pageType <= 38) {
                  response.getWriter().println("<p><a href=\"page" + i + ".html\">infinite " + i + "</a></p>");
                } else {
                  response.getWriter().println("<p><a href=\"page" + i + ".html\">ajaxtimeout " + i + "</a></p>");
                }
              }
              response.getWriter().println("</body></html>");
            }
          }
        });
        server.start();
        server.join();
      } catch (Exception e) {
        System.err.println("Couldn't create embedded jetty server");
        e.printStackTrace();
      }
    });

    httpServerThread.start();

    JBrowserDriver driver = new JBrowserDriver(Settings.builder().
        timezone(Timezone.AMERICA_NEWYORK).
        ajaxWait(50000L).
        ajaxResourceTimeout(50000L).
        connectTimeout(50000).
        connectionReqTimeout(50000).
        socketTimeout(50000).
        build());


    String host = InetAddress.getLocalHost().getHostAddress();

    for (int i=0; i<30; ++i) {
      Stopwatch stopwatch = Stopwatch.createStarted();
      int nextIdx = new Random().nextInt(10000);
      String nextUrl = "http://" + host + ":" + mainPort + "/page" + nextIdx + ".html";

      System.out.println("=== " + nextUrl + " ===");
      driver.get(nextUrl);
      long stopwatchTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
      System.out.println(driver.getStatusCode() + " in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");
      String src = driver.getPageSource();

      StringTokenizer st = new StringTokenizer(src, "\n");
      String timeTakenStr = "time taken ms = [";
      long maxTime = 0;
      while (st.hasMoreTokens()) {
        String nextLine = st.nextToken();
        if (nextLine.contains(timeTakenStr) && nextLine.contains("div>")) {
          Long timeTaken = Long.parseLong(nextLine.substring(nextLine.indexOf("[")+1, nextLine.indexOf("]")));
          maxTime = Long.max(timeTaken, maxTime);
        }
      }
      Assert.assertTrue(String.format("Time taken to process javascript page %d should be at most 1s more than time " +
          "taken for the  longest ajax request %d", stopwatchTime, maxTime), stopwatchTime < maxTime + 1000);
      System.out.println("=== END " + nextUrl + " ===");
      driver.quit();
    }


  }
}
