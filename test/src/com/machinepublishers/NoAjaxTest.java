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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class NoAjaxTest {
  @Test
  public void testNoAjax() throws Exception {

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
            // vary between xhtml manual ajax and jquery ajax
            response.getWriter().println("<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<body>\n" +
                "\n" +
                "<div id=\"demo1\">\n" +
                "  <h2>done already</h2>\n" +
                "</div>\n" +
                "<div id=\"demo2\">\n" +
                "  <h2>done already</h2>\n" +
                "</div>\n" +
                "<div id=\"demo3\">\n" +
                "  <h2>done already</h2>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>");
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
        loggerLevel(Level.FINEST).
        quickRender(true).
        logWire(true).
        logTrace(true).
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
      Assert.assertTrue(String.format("Time taken to process javascript page %d should be at most 3s", stopwatchTime)
          , stopwatchTime < 3000);
      System.out.println("=== END " + nextUrl + " ===");
      driver.quit();
    }


  }
}
