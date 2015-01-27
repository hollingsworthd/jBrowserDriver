package com.machinepublishers.jbrowserdriver;

import com.machinepublishers.jbrowserdriver.config.Proxy;
import com.machinepublishers.jbrowserdriver.config.Settings;

public class SimpleTest {
  public static void main(String[] args) {
    //    final JBrowserDriver browser1 = new JBrowserDriver(new Settings(null, null, null, new Proxy(Proxy.Type.SOCKS, "127.0.0.1", 9050, null, null)));
    final JBrowserDriver browser1 = new JBrowserDriver(new Settings(null, null, null, new Proxy(Proxy.Type.HTTP, "us-il.proxymesh.com", 31280, "tcomx4", "ca58da6c57dfa7e0f256")));
    browser1.init();
    //    Browser browser2 = new JBrowserDriver();
    final JBrowserDriver browser3 = new JBrowserDriver(new Settings(null, null, null, new Proxy(Proxy.Type.HTTP, "us-il.proxymesh.com", 31280, "tcomx4", "ca58da6c57dfa7e0f256")));
    browser3.init();
    //    browser.get("http://myproxylists.com/my-http-headers");
    //    http: //www.andlabs.org/tools/jsrecon.html
    //    browser.get("http://www.screenresolution.org/");
    //    browser.get("https://www.browserleaks.com/javascript");
    //    browser.get("http://myproxylists.com/my-http-headers");
    //    browser.get("http://jsfiddle.net/sjumpupm/");
    //    System.out.println(browser.getPageSource());
    //    browser.get("http://www.mydevice.io/");
    //    try {
    //      Thread.sleep(4000);
    //    } catch (InterruptedException e) {
    //      // TODO Auto-generated catch block
    //      e.printStackTrace();
    //    }
    //    System.out.println(browser.getScreenshotAs(OutputType.FILE).getAbsolutePath());
    //    try {
    //      Thread.sleep(4000);
    //    } catch (InterruptedException e) {
    //      // TODO Auto-generated catch block
    //      e.printStackTrace();
    //    }
    //    browser.close();
    //    Browser browser2 = new JBrowserDriver(new Settings(null, null, new BrowserProperties(true, new Dimension(800, 600), null, null, null), null));
    //    browser2.get("http://www.mydevice.io/");
    //    try {
    //      Thread.sleep(4000);
    //    } catch (InterruptedException e) {
    //      // TODO Auto-generated catch block
    //      e.printStackTrace();
    //    }
    //    System.out.println(browser2.getScreenshotAs(OutputType.FILE).getAbsolutePath());
    //    try {
    //      Thread.sleep(4000);
    //    } catch (InterruptedException e) {
    //      // TODO Auto-generated catch block
    //      e.printStackTrace();
    //    }
    //    System.out.println(browser.getScreenshotAs(OutputType.FILE).getAbsolutePath());
    //    new Thread(new Runnable() {
    //      @Override
    //      public void run() {
    //        browser1.get("jbd://grepcode.com/file/repo1.maven.org/maven2/net.java.openjfx.backport/openjfx-78-backport/1.8.0-ea-b96.1/com/sun/webkit/WebPage.java?av=f");
    //      }
    //    }).start();
    new Thread(new Runnable() {
      @Override
      public void run() {
        browser1.get("jbds1://facebook.com");
      }
    }).start();
    new Thread(new Runnable() {
      @Override
      public void run() {
        browser3.get("jbds2://facebook.com");
      }
    }).start();

    //    browser2.get("https://www.browserleaks.com/whois");
    //    browser.get("http://www.screenslicer.com/");
    //    browser.get("https://github.com/blog");
    //    System.out.println(browser.getPageSource());
  }
}
