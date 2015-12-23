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

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;

import com.machinepublishers.jbrowserdriver.StreamInjectors.Injector;

import javafx.embed.swing.JFXPanel;

/**
 * An immutable class which contains settings for the browser.
 * 
 * @see Settings#builder()
 * @see JBrowserDriver#JBrowserDriver(Settings)
 */
public class Settings implements Serializable {
  /**
   * A script to guard against canvas fingerprinting and also add some typical navigator properties.
   */
  public static final String HEAD_SCRIPT;

  static {
    StringBuilder builder = new StringBuilder();
    builder.append("Object.defineProperty(window, 'external', ");
    builder.append("{value:{AddSearchProvider:function(){},IsSearchProviderInstalled:function(){},addSearchEngine:function(){}}});");
    builder.append("Object.defineProperty(window.external.AddSearchProvider, 'toString', ");
    builder.append("{value:function(){return 'function AddSearchProvider() { [native code] }';}});");
    builder.append("Object.defineProperty(window.external.IsSearchProviderInstalled, 'toString', ");
    builder.append("{value:function(){return 'function IsSearchProviderInstalled() { [native code] }';}});");
    builder.append("Object.defineProperty(window.external.addSearchEngine, 'toString', ");
    builder.append("{value:function(){return 'function addSearchEngine() { [native code] }';}});");

    //TODO FIXME handle shift key event in case of headless browser
    //    builder.append("(function(){");
    //    builder.append("var windowOpen = window.open;Object.defineProperty(window,'open',");
    //    builder.append("{value:function(url, target, props){if(event && event.shiftKey){windowOpen(url);}else{windowOpen(url,'_self');}}}");
    //    builder.append(")})();");

    builder.append("Object.defineProperty(HTMLCanvasElement.prototype, ");
    builder.append("'toBlob', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(HTMLCanvasElement.prototype, ");
    builder.append("'toDataURL', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, ");
    builder.append("'createImageData', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, ");
    builder.append("'getImageData', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, ");
    builder.append("'measureText', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, ");
    builder.append("'isPointInPath', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, ");
    builder.append("'isPointInStroke', {value:function(){return undefined;}});");
    HEAD_SCRIPT = builder.toString();
  }

  private static final Random rand = new Random();
  private static final boolean headless;

  static {
    if (!"true".equals(System.getProperty("jbd.browsergui"))) {
      headless = true;
      System.setProperty("glass.platform", "Monocle");
      System.setProperty("monocle.platform", "Headless");
      System.setProperty("prism.order", "sw");
      System.setProperty("prism.useFontConfig", "false");
      System.setProperty("prism.subpixeltext", "false");
      System.setProperty("prism.allowhidpi", "false");
      try {
        Class<?> platformFactory = Class.forName("com.sun.glass.ui.PlatformFactory");
        Field field = platformFactory.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(platformFactory, Class.forName(
            "com.sun.glass.ui.monocle.MonoclePlatformFactory").newInstance());

        platformFactory = Class.forName("com.sun.glass.ui.monocle.NativePlatformFactory");
        field = platformFactory.getDeclaredField("platform");
        field.setAccessible(true);
        Constructor headlessPlatform = Class.forName("com.sun.glass.ui.monocle.HeadlessPlatform").getDeclaredConstructor();
        headlessPlatform.setAccessible(true);
        field.set(platformFactory, headlessPlatform.newInstance());
      } catch (Throwable t) {
        Logs.logsFor(1l).exception(t);
      }
    } else {
      headless = false;
      new JFXPanel();
    }
    try {
      URL.setURLStreamHandlerFactory(new StreamHandler());
    } catch (Throwable t) {
      Field factory = null;
      try {
        factory = URL.class.getDeclaredField("factory");
        factory.setAccessible(true);
        Object curFac = factory.get(null);

        //assume we're in the Eclipse jar-in-jar loader
        Field chainedFactory = curFac.getClass().getDeclaredField("chainFac");
        chainedFactory.setAccessible(true);
        chainedFactory.set(curFac, new StreamHandler());
      } catch (Throwable t2) {
        try {
          //this should work regardless
          factory.set(null, new StreamHandler());
        } catch (Throwable t3) {}
      }
    }
    final Pattern head = Pattern.compile("<head\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Pattern html = Pattern.compile("<html\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Pattern body = Pattern.compile("<body\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    StreamInjectors.add(new Injector() {
      @Override
      public byte[] inject(StreamConnection connection,
          byte[] inflatedContent, String originalUrl, long settingsId) {
        AtomicReference<Settings> settings = SettingsManager.get(settingsId);
        try {
          if (settings.get().saveMedia()
              && connection.isMedia()) {
            String filename = Long.toString(System.nanoTime());
            File contentFile = new File(settings.get().mediaDir(), filename + ".content");
            File metaFile = new File(settings.get().mediaDir(), filename + ".metadata");
            while (contentFile.exists() || metaFile.exists()) {
              filename = Long.toString(Math.abs(rand.nextLong()));
              contentFile = new File(settings.get().mediaDir(), filename + ".content");
              metaFile = new File(settings.get().mediaDir(), filename + ".metadata");
            }
            contentFile.deleteOnExit();
            metaFile.deleteOnExit();
            Files.write(contentFile.toPath(), inflatedContent);
            Files.write(metaFile.toPath(),
                (originalUrl + "\n" + connection.getContentType()).getBytes("utf-8"));
          }
        } catch (Throwable t) {}
        try {
          if (!"false".equals(System.getProperty("jbd.quickrender"))
              && connection.isMedia()) {
            Logs.logsFor(settingsId).trace("Media discarded: " + connection.getURL().toExternalForm());
            StatusMonitor.get(settingsId).addDiscarded(connection.getURL().toExternalForm());
            return new byte[0];
          } else if ((connection.getContentType() == null || connection.getContentType().indexOf("text/html") > -1)
              && StatusMonitor.get(settingsId).isPrimaryDocument(connection.getURL().toExternalForm())) {
            String injected = null;
            String charset = Util.charset(connection);
            String content = new String(inflatedContent, charset);
            Matcher matcher = head.matcher(content);
            if (matcher.find()) {
              injected = matcher.replaceFirst(matcher.group(0) + settings.get().script());
            } else {
              matcher = html.matcher(content);
              if (matcher.find()) {
                injected = matcher.replaceFirst(
                    matcher.group(0) + "<head>" + settings.get().script() + "</head>");
              } else {
                matcher = body.matcher(content);
                if (matcher.find()) {
                  injected = ("<html><head>" + settings.get().script() + "</head>"
                      + content + "</html>");
                }
              }
            }
            return injected == null ? null : injected.getBytes(charset);
          }
        } catch (Throwable t) {}
        return null;
      }
    });
  }

  /**
   * Convenience method for getting a Settings builder, which
   * configures jBrowserDriver. Settings objects can safely be re-used
   * across different instances of {@link JBrowserDriver}.
   * <p>This is equivalent to calling <code> new Settings.Builder()</code>
   * 
   * @return Settings.Builder
   * @see JBrowserDriver#JBrowserDriver(Settings)
   */
  public static Settings.Builder builder() {
    return new Settings.Builder();
  }

  /**
   * Helps build a Settings object which configures jBrowserDriver.
   * Settings objects can safely be re-used
   * across different instances of {@link JBrowserDriver}.
   * 
   * @see JBrowserDriver#JBrowserDriver(Settings)
   */
  public static class Builder {
    private RequestHeaders requestHeaders = RequestHeaders.TOR;
    private Dimension screen = new Dimension(1000, 600);
    private UserAgent userAgent = UserAgent.TOR;
    private Timezone timezone = Timezone.UTC;
    private String headScript = HEAD_SCRIPT;
    private ProxyConfig proxy = new ProxyConfig();
    private File downloadDir = new File("./download_cache");
    private File mediaDir = new File("./media_cache");
    private boolean saveMedia;
    private boolean saveMediaInit;

    /**
     * @param requestHeaders
     *          Headers to be sent on each request. Defaults to {@link RequestHeaders#TOR}.
     * @return this Builder
     * @see Builder#userAgent(UserAgent)
     */
    public Builder requestHeaders(RequestHeaders requestHeaders) {
      this.requestHeaders = requestHeaders;
      return this;
    }

    /**
     * @param screen
     *          Screen and window size. Defaults to 1000x600. This is a typical size for Tor Browser
     *          but if you're not using the Tor user agent you might want to specify a more common
     *          size such as 1366x768.
     * @return this Builder
     */
    public Builder screen(Dimension screen) {
      this.screen = screen;
      return this;
    }

    /**
     * @param userAgent
     *          Browser's user agent and related properties. Defaults to {@link UserAgent#TOR}.
     * @return this Builder
     * @see Builder#requestHeaders(RequestHeaders)
     */
    public Builder userAgent(UserAgent userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    /**
     * @param timezone
     *          Timezone of the browser. Defaults to {@link Timezone#UTC}. This is the timezone
     *          of Tor Browser but if you're not using the Tor user agent you might want to
     *          use a locale nearer your actual computer, such as {@link Timezone#AMERICA_LOSANGELES} if you're in San Francisco.
     * @return this Builder
     */
    public Builder timezone(Timezone timezone) {
      this.timezone = timezone;
      return this;
    }

    /**
     * @param headScript
     *          Script to be injected in the HTML Head section.
     *          Omit &lt;script&gt; tags; they will be added automatically.
     *          Defaults to {@link Settings#HEAD_SCRIPT}.
     * @return this Builder
     */
    public Builder headScript(String headScript) {
      this.headScript = headScript;
      return this;
    }

    /**
     * @param proxy
     *          Proxy server to be used. Defaults to a direct connection (no proxy).
     * @return this Builder
     */
    public Builder proxy(ProxyConfig proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Specifies the directory to save downloaded files. If not specified then
     * ./download_cache is created and used. Downloaded files are deleted when the
     * JVM exits, so copy them to a different location to persist them permanently.
     * 
     * @param downloadDir
     *          Where to save downloaded files
     * @return this Builder
     */
    public Builder downloadDir(File downloadDir) {
      this.downloadDir = downloadDir;
      return this;
    }

    /**
     * Specifies the directory to save media files. Calling this method with a non-null
     * File implicitly calls Builder.saveMedia(true) unless you've already called
     * Builder.saveMedia(false). If you do not set a directory here and you've called
     * Builder.saveMedia(true) then the directory ./media_cache is created and used.
     * <p>
     * Media is saved as two files in the mediaDir: &lt;identifier&gt;.content (which
     * has the binary content) and &lt;identifier&gt;.metadata (the first line of this
     * file is the original URL of the request for this media, and the second line is
     * the mime type).
     * <p>
     * Saved media files are deleted when the JVM exits, so copy them to a different
     * location to persist them permanently.
     * <p>
     * 
     * @param mediaDir
     *          Where to save media files.
     * @return this Builder
     */
    public Builder mediaDir(File mediaDir) {
      this.mediaDir = mediaDir;
      if (mediaDir != null && !saveMediaInit) {
        this.saveMedia = true;
      }
      return this;
    }

    /**
     * Whether to save media (e.g., images) to disk. Defaults to false but calls to
     * Builder.mediaDir(File) implicitly call Builder.saveMedia(true) unless
     * the File is null or Builder.saveMedia(false) was previously called by you.
     * <p>
     * Media is saved as two files in the mediaDir: &lt;identifier&gt;.content (which
     * has the binary content) and &lt;identifier&gt;.metadata (the first line of this
     * file is the original URL of the request for this media, and the second line is
     * the mime type).
     * <p>
     * Saved media files are deleted when the JVM exits, so copy them to a different
     * location to persist them permanently.
     * <p>
     * 
     * @param saveMedia
     *          Whether to save media (e.g., images) to disk.
     * @return this Builder
     */
    public Builder saveMedia(boolean saveMedia) {
      this.saveMedia = saveMedia;
      this.saveMediaInit = true;
      return this;
    }

    /**
     * @return A Settings object created from this builder.
     * @see JBrowserDriver#JBrowserDriver(Settings)
     */
    public Settings build() {
      return new Settings(this.requestHeaders, this.screen, this.userAgent, this.timezone,
          this.headScript, this.proxy, this.downloadDir, this.mediaDir, this.saveMedia);
    }
  }

  private final RequestHeaders requestHeaders;
  private final int screenWidth;
  private final int screenHeight;
  private final String userAgentString;
  private final ProxyConfig proxy;
  private final File downloadDir;
  private final File mediaDir;
  private final boolean saveMedia;
  private static final AtomicLong settingsId = new AtomicLong();
  private final long mySettingsId;
  private final String script;
  private transient final CookieStore cookieStore = new BasicCookieStore();

  /**
   * Do not call this constructor. Instead, see {@link Settings#builder()}.
   */
  Settings() {
    this(new Builder().build());
  }

  private Settings(final RequestHeaders requestHeaders, final Dimension screen,
      final UserAgent userAgent, final Timezone timezone,
      final String headScript, final ProxyConfig proxy,
      final File downloadDir, final File mediaDir, final boolean saveMedia) {
    mySettingsId = -1;
    this.requestHeaders = requestHeaders;
    this.screenWidth = screen.getWidth();
    this.screenHeight = screen.getHeight();
    this.userAgentString = userAgent.userAgentString();
    this.proxy = proxy;
    this.downloadDir = downloadDir;
    if (!this.downloadDir.exists()) {
      this.downloadDir.mkdirs();
      this.downloadDir.deleteOnExit();
    }
    this.mediaDir = mediaDir;
    if (!this.mediaDir.exists()) {
      this.mediaDir.mkdirs();
      this.mediaDir.deleteOnExit();
    }
    this.saveMedia = saveMedia;

    StringBuilder scriptBuilder = new StringBuilder();
    String scriptId = "A" + rand.nextLong();
    if (headless) {
      scriptBuilder.append("<style>body::-webkit-scrollbar {width: 0px !important;height:0px !important;}</style>");
    }
    scriptBuilder.append("<script id='" + scriptId + "' language='javascript'>");
    scriptBuilder.append("try{");
    scriptBuilder.append(userAgent.script());
    scriptBuilder.append(timezone.script());
    if (headScript != null) {
      scriptBuilder.append(headScript);
    }
    scriptBuilder.append("}catch(e){}");
    scriptBuilder.append("document.getElementsByTagName('head')[0].removeChild(document.getElementById('" + scriptId + "'));");
    scriptBuilder.append("</script>");
    script = scriptBuilder.toString();
  }

  Settings(Settings original) {
    requestHeaders = original.requestHeaders;
    screenWidth = original.screenWidth;
    screenHeight = original.screenHeight;
    userAgentString = original.userAgentString;
    proxy = original.proxy;
    downloadDir = original.downloadDir;
    mediaDir = original.mediaDir;
    saveMedia = original.saveMedia;
    mySettingsId = settingsId.incrementAndGet();
    script = original.script;
  }

  Settings(Settings original, long settingsId) {
    requestHeaders = original.requestHeaders;
    screenWidth = original.screenWidth;
    screenHeight = original.screenHeight;
    userAgentString = original.userAgentString;
    proxy = original.proxy;
    downloadDir = original.downloadDir;
    mediaDir = original.mediaDir;
    saveMedia = original.saveMedia;
    mySettingsId = settingsId;
    script = original.script;
  }

  long id() {
    return mySettingsId;
  }

  RequestHeaders headers() {
    return requestHeaders;
  }

  int screenWidth() {
    return screenWidth;
  }

  int screenHeight() {
    return screenWidth;
  }

  String userAgentString() {
    return userAgentString;
  }

  ProxyConfig proxy() {
    return proxy;
  }

  File downloadDir() {
    return downloadDir;
  }

  File mediaDir() {
    return mediaDir;
  }

  boolean saveMedia() {
    return saveMedia;
  }

  String script() {
    return script;
  }

  CookieStore cookieStore() {
    return cookieStore;
  }

  static boolean headless() {
    return headless;
  }
}
