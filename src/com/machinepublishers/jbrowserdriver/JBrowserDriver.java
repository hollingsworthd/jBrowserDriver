/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.HasInputDevices;
import org.openqa.selenium.internal.FindsByClassName;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.openqa.selenium.internal.Killable;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.sun.javafx.webkit.Accessor;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

/**
 * Use this library like any other Selenium WebDriver or RemoteWebDriver (it implements Selenium's
 * JavascriptExecutor, HasInputDevices, TakesScreenshot, Killable, FindsById, FindsByClassName,
 * FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName, and FindsByXPath).
 * <p>
 * You can optionally pass a {@link Settings} object to the {@link JBrowserDriver#JBrowserDriver(Settings)}
 * constructor to specify a proxy, request headers, time zone, user agent, or navigator details.
 * By default, the browser mimics the fingerprint of Tor Browser.
 * <p>
 * Also, you can run as many instances of JBrowserDriver as you want (it's thread safe), and the
 * browser sessions will be fully isolated from each other when run in headless mode, which is the
 * default (when it's run with the GUI shown, some of the memory between instances is shared out of
 * necessity).
 * <p>
 * Example:
 * 
 * <pre>
 * import org.openqa.selenium.WebDriver;
 * import com.machinepublishers.jbrowserdriver.Timezone;
 * import com.machinepublishers.jbrowserdriver.JBrowserDriver;
 * import com.machinepublishers.jbrowserdriver.Settings;
 * 
 * public class Example {
 *   public static void main(String[] args) {
 * 
 *     // You can optionally pass a Settings object here,
 *     // constructed using Settings.Builder
 *     WebDriver driver = new JBrowserDriver(Settings.builder().timezone(Timezone.AMERICA_NEWYORK).build());
 * 
 *     // This will block for the page load and any
 *     // associated AJAX requests
 *     driver.get("http://example.com");
 * 
 *     // You can get status code unlike other Selenium drivers.
 *     // It blocks for AJAX requests and page loads after clicks
 *     // and keyboard events.
 *     System.out.println(((JBrowserDriver) driver).getStatusCode());
 * 
 *     // Returns the page source in its current state, including
 *     // any DOM updates that occurred after page load
 *     System.out.println(driver.getPageSource());
 *   }
 * }
 * </pre>
 * 
 * <b>Java System Properties:</b>
 * <p>
 * <b>jbd.traceconsole</b> Mirror trace-level log messages to standard out.
 * Otherwise these logs are only available through the Selenium APIs.Defaults to <b>false</b>.
 * <p>
 * <b>jbd.warnconsole</b> Mirror warning-level log messages to standard error. Otherwise
 * these logs are only available through the Selenium APIs. Defaults to <b>true</b>.
 * <p>
 * <b>jbd.maxlogs</b> Maximum number of log entries to store in memory, accessible via the
 * Selenium APIs. Oldest log entry is dropped once max is reached. Regardless of this
 * setting, logs are cleared per instance of JBrowserDriver after a call to quit(), reset(),
 * or Logs.get(String). Defaults to <b>5000</b>.
 * <p>
 * <b>jbd.browsergui</b> Show the browser GUI window. Defaults to <b>false</b>.
 * <p>
 * <b>jbd.quickrender</b> Exclude web page images and binary data from rendering. These
 * resources are still requested and can optionally be saved to disk (see the Settings
 * options). Some versions of Java are inefficient (memory-wise) in rendering images.
 * Defaults to <b>true</b>.
 * <p>
 * <b>jbd.blockads</b> Whether requests to ad/spam servers should be blocked. Based on hosts
 * in ad-hosts.txt in the source tree. Defaults to <b>true</b>.
 * <p>
 * <b>jbd.ajaxwait</b> The idle time (no pending AJAX requests) required in milliseconds
 * before a page is considered to have been loaded completely. For very slow or overloaded
 * CPUs, set a higher value. Defaults to <b>120</b>.
 * <p>
 * <b>jbd.ajaxresourcetimeout</b> The time in milliseconds after which an AJAX request will
 * be ignored when considering whether all AJAX requests have completed. Defaults to <b>2000</b>.
 * <p>
 * <b>jbd.pemfile</b> Specifies a source of trusted certificate authorities. Can take one of
 * four values: (1) <b>compatible</b> to accept standard browser certs, (2) <b>trustanything</b>
 * to accept any SSL cert, (3) a file path, or (4) a URL. The default when this property is not
 * set is your JRE's keystore, so you can use JDK's keytool to import specific certs.
 * <p>
 * <b>jbd.maxrouteconnections</b> Maximum number of concurrent connections to a specific
 * host+proxy combo. Defaults to <b>8</b>.
 * <p>
 * <b>jbd.maxconnections</b> Maximum number of concurrent connections overall.
 * Defaults to <b>3000</b>.
 * <p>
 */
public class JBrowserDriver implements WebDriver, JavascriptExecutor, FindsById,
    FindsByClassName, FindsByLinkText, FindsByName, FindsByCssSelector, FindsByTagName,
    FindsByXPath, HasInputDevices, HasCapabilities, TakesScreenshot, Killable {

  /**
   * Use this string on sendKeys functions to delete text.
   */
  public static final String KEYBOARD_DELETE;

  static {
    final int CHARS_TO_DELETE = 60;
    StringBuilder builder = new StringBuilder();
    String key = Keys.BACK_SPACE.toString();
    for (int i = 0; i < CHARS_TO_DELETE; i++) {
      builder.append(key);
    }
    key = Keys.DELETE.toString();
    for (int i = 0; i < CHARS_TO_DELETE; i++) {
      builder.append(key);
    }
    KEYBOARD_DELETE = builder.toString();
  }

  final Context context;

  /**
   * Constructs a browser with default settings, UTC timezone, and no proxy.
   */
  public JBrowserDriver() {
    this(Settings.builder().build());
  }

  /**
   * Use Settings.Builder to create settings to pass to this constructor.
   * 
   * @param settings
   */
  public JBrowserDriver(final Settings settings) {
    context = new Context(new Settings(settings));
  }

  /**
   * Optionally call this method if you want JavaFX initialized and the browser
   * window opened immediately. Otherwise, initialization will happen lazily.
   */
  public void init() {
    context.init(this);
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   * 
   * @param settings
   *          New settings to take effect, superseding the original ones
   */
  public void reset(final Settings settings) {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        context.item().engine.get().getLoadWorker().cancel();
        return null;
      }
    }, context.settingsId.get());
    Accessor.getPageFor(context.item().engine.get()).stop();
    context.settings.set(new Settings(settings, context.settingsId.get()));
    context.reset(this);
    context.settings.get().cookieStore().clear();
    StatusMonitor.get(context.settings.get().id()).clearStatusMonitor();
    context.logs.get().clear();
  }

  /**
   * Reset the state of the browser. More efficient than quitting the
   * browser and creating a new instance.
   */
  public void reset() {
    reset(context.settings.get());
  }

  @Override
  public String getPageSource() {
    init();
    return Element.create(context).findElementByTagName("html").getAttribute("outerHTML");
  }

  @Override
  public String getCurrentUrl() {
    init();
    return Util.exec(Pause.NONE, context.statusCode, new Sync<String>() {
      public String perform() {
        return context.item().view.get().getEngine().getLocation();
      }
    }, context.settingsId.get());
  }

  public int getStatusCode() {
    init();
    try {
      synchronized (context.statusCode) {
        if (context.statusCode.get() == 0) {
          context.statusCode.wait(context.timeouts.get().getPageLoadTimeoutMS());
        }
      }
    } catch (InterruptedException e) {
      context.logs.get().exception(e);
    }
    return context.statusCode.get();
  }

  @Override
  public String getTitle() {
    init();
    return Util.exec(Pause.NONE, context.statusCode, new Sync<String>() {
      public String perform() {
        return context.item().view.get().getEngine().getTitle();
      }
    }, context.settingsId.get());
  }

  @Override
  public void get(final String url) {
    init();
    Util.exec(Pause.SHORT, context.statusCode, new Sync<Object>() {
      public Object perform() {
        context.item().engine.get().load(url);
        return null;
      }
    }, context.settingsId.get());
    try {
      synchronized (context.statusCode) {
        if (context.statusCode.get() == 0) {
          context.statusCode.wait(context.timeouts.get().getPageLoadTimeoutMS());
        }
      }
    } catch (InterruptedException e) {
      context.logs.get().exception(e);
    }
    if (context.statusCode.get() == 0) {
      Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
        @Override
        public Object perform() {
          context.item().engine.get().getLoadWorker().cancel();
          return null;
        }
      }, context.settingsId.get());
    }
  }

  @Override
  public WebElement findElement(By by) {
    init();
    return by.findElement(this);
  }

  @Override
  public List<WebElement> findElements(By by) {
    init();
    return by.findElements(this);
  }

  @Override
  public WebElement findElementById(String id) {
    init();
    return Element.create(context).findElementById(id);
  }

  @Override
  public List<WebElement> findElementsById(String id) {
    init();
    return Element.create(context).findElementsById(id);
  }

  @Override
  public WebElement findElementByXPath(String expr) {
    init();
    return Element.create(context).findElementByXPath(expr);
  }

  @Override
  public List<WebElement> findElementsByXPath(String expr) {
    init();
    return Element.create(context).findElementsByXPath(expr);
  }

  @Override
  public WebElement findElementByLinkText(final String text) {
    init();
    return Element.create(context).findElementByLinkText(text);
  }

  @Override
  public WebElement findElementByPartialLinkText(String text) {
    init();
    return Element.create(context).findElementByPartialLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByLinkText(String text) {
    init();
    return Element.create(context).findElementsByLinkText(text);
  }

  @Override
  public List<WebElement> findElementsByPartialLinkText(String text) {
    init();
    return Element.create(context).findElementsByPartialLinkText(text);
  }

  @Override
  public WebElement findElementByClassName(String cssClass) {
    init();
    return Element.create(context).findElementByClassName(cssClass);
  }

  @Override
  public List<WebElement> findElementsByClassName(String cssClass) {
    init();
    return Element.create(context).findElementsByClassName(cssClass);
  }

  @Override
  public WebElement findElementByName(String name) {
    init();
    return Element.create(context).findElementByName(name);
  }

  @Override
  public List<WebElement> findElementsByName(String name) {
    init();
    return Element.create(context).findElementsByName(name);
  }

  @Override
  public WebElement findElementByCssSelector(String expr) {
    init();
    return Element.create(context).findElementByCssSelector(expr);
  }

  @Override
  public List<WebElement> findElementsByCssSelector(String expr) {
    init();
    return Element.create(context).findElementsByCssSelector(expr);
  }

  @Override
  public WebElement findElementByTagName(String tagName) {
    init();
    return Element.create(context).findElementByTagName(tagName);
  }

  @Override
  public List<WebElement> findElementsByTagName(String tagName) {
    init();
    return Element.create(context).findElementsByTagName(tagName);
  }

  @Override
  public Object executeAsyncScript(String script, Object... args) {
    init();
    return Element.create(context).executeAsyncScript(script, args);
  }

  @Override
  public Object executeScript(String script, Object... args) {
    init();
    return Element.create(context).executeScript(script, args);
  }

  @Override
  public Keyboard getKeyboard() {
    init();
    return context.keyboard.get();
  }

  @Override
  public Mouse getMouse() {
    init();
    return context.mouse.get();
  }

  @Override
  public Capabilities getCapabilities() {
    init();
    return context.capabilities.get();
  }

  @Override
  public void close() {
    init();
    context.removeItem();
  }

  @Override
  public String getWindowHandle() {
    init();
    return context.itemId();
  }

  @Override
  public Set<String> getWindowHandles() {
    init();
    return context.itemIds();
  }

  @Override
  public Options manage() {
    init();
    return context.options.get();
  }

  @Override
  public Navigation navigate() {
    init();
    return context.item().navigation.get();
  }

  @Override
  public void quit() {
    Util.exec(Pause.SHORT, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        context.item().engine.get().getLoadWorker().cancel();
        return null;
      }
    }, context.settingsId.get());
    Accessor.getPageFor(context.item().engine.get()).stop();
    if (Settings.headless()) {
      Platform.exit();
    }
    SettingsManager.close(context.settings.get().id());
    context.settings.get().cookieStore().clear();
    StatusMonitor.get(context.settings.get().id()).clearStatusMonitor();
    StatusMonitor.remove(context.settings.get().id());
    Logs.close(context.settingsId.get());
  }

  @Override
  public TargetLocator switchTo() {
    init();
    return context.targetLocator.get();
  }

  @Override
  public void kill() {
    quit();
  }

  @Override
  public <X> X getScreenshotAs(final OutputType<X> outputType) throws WebDriverException {
    init();
    BufferedImage image = Util.exec(Pause.NONE, context.statusCode, new Sync<BufferedImage>() {
      public BufferedImage perform() {
        return SwingFXUtils.fromFXImage(
            context.item().view.get().snapshot(
                new SnapshotParameters(),
                new WritableImage(
                    (int) Math.rint((Double) context.item().view.get().getWidth()),
                    (int) Math.rint((Double) context.item().view.get().getHeight()))),
            null);
      }
    }, context.settingsId.get());
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      ImageIO.write(image, "png", out);
      return outputType.convertFromPngBytes(out.toByteArray());
    } catch (Throwable t) {
      context.logs.get().exception(t);
      return null;
    } finally {
      Util.close(out);
    }
  }
}
