/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.io.File;
import java.lang.reflect.Field;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.machinepublishers.jbrowserdriver.StreamInjectors.Injector;

public class Settings {
  private static final boolean headless;
  static {
    if (!"true".equals(System.getProperty("jbd.browsergui"))) {
      headless = true;
      System.setProperty("glass.platform", "Monocle");
      System.setProperty("monocle.platform", "Headless");
      System.setProperty("prism.order", "sw");
      //AWT gui is never used anyhow, but set this to prevent programmer errors
      System.setProperty("java.awt.headless", "true");
    } else {
      headless = false;
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
      public byte[] inject(HttpURLConnection connection,
          byte[] inflatedContent, String originalUrl, long settingsId) {
        AtomicReference<Settings> settings = SettingsManager.get(settingsId);
        try {
          if (settings.get().saveMedia()
              && StreamConnection.isMedia(connection.getContentType())) {
            File file = new File(settings.get().mediaDir(),
                Base64.getEncoder().encodeToString(
                    originalUrl.replaceFirst("^https?://", "").getBytes("utf-8")));
            file.deleteOnExit();
            Files.write(file.toPath(), inflatedContent);
          }
        } catch (Throwable t) {}
        try {
          if (!"false".equals(System.getProperty("jbd.quickrender"))
              && StreamConnection.isMedia(connection.getContentType())) {
            if (Logs.TRACE) {
              System.out.println("Media discarded: " + connection.getURL().toExternalForm());
            }
            StatusMonitor.get(settingsId).addDiscarded(connection.getURL().toExternalForm());
            return new byte[0];
          } else if (
          (connection.getContentType() == null || connection.getContentType().indexOf("text/html") > -1)
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
   * Helps build the Settings object.
   */
  public static class Builder {
    private RequestHeaders requestHeaders;
    private BrowserTimeZone browserTimeZone;
    private BrowserProperties browserProperties;
    private ProxyConfig proxy;
    private File downloadDir;
    private File mediaDir;
    private boolean saveMedia;
    private boolean saveMediaInit;

    /**
     * @param requestHeaders
     *          Headers to be sent on each request
     * @return this Builder
     */
    public Builder requestHeaders(RequestHeaders requestHeaders) {
      this.requestHeaders = requestHeaders;
      return this;
    }

    /**
     * @param browserTimeZone
     *          Timezone of the browser
     * @return this Builder
     */
    public Builder browserTimeZone(BrowserTimeZone browserTimeZone) {
      this.browserTimeZone = browserTimeZone;
      return this;
    }

    /**
     * @param browserProperties
     *          Various DOM and JavaScript properties
     * @return this Builder
     */
    public Builder browserProperties(BrowserProperties browserProperties) {
      this.browserProperties = browserProperties;
      return this;
    }

    /**
     * @param proxy
     *          Proxy server to be used
     * @return this Builder
     */
    public Builder proxy(ProxyConfig proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * @param downloadDir
     *          Where to save downloaded files
     * @return this Builder
     */
    public Builder downloadDir(File downloadDir) {
      this.downloadDir = downloadDir;
      return this;
    }

    /**
     * @param mediaDir
     *          Where to save media files
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
     * @param saveMedia
     *          Whether to download media (e.g., images).
     *          If so, they're saved in the downloadDir
     *          and the filename is its base64-encoded URL
     *          with the leading "http://" or "https://" stripped.
     * @return this Builder
     */
    public Builder saveMedia(boolean saveMedia) {
      this.saveMedia = saveMedia;
      this.saveMediaInit = true;
      return this;
    }

    /**
     * @return A Settings object created from this builder.
     */
    public Settings build() {
      return new Settings(this.requestHeaders, this.browserTimeZone, this.browserProperties,
          this.proxy, this.downloadDir, this.mediaDir, this.saveMedia);
    }
  }

  private static final Random rand = new Random();
  private final RequestHeaders requestHeaders;
  private final BrowserTimeZone browserTimeZone;
  private final BrowserProperties browserProperties;
  private final ProxyConfig proxy;
  private final File downloadDir;
  private final File mediaDir;
  private final boolean saveMedia;
  private static final AtomicLong settingsId = new AtomicLong();
  private final long mySettingsId;
  private final String script;
  private final CookieManager cookieManager = new CookieManager();

  Settings() {
    this(null, null, null, null, null, null, false);
  }

  private Settings(final RequestHeaders requestHeaders, final BrowserTimeZone browserTimeZone,
      final BrowserProperties browserProperties, final ProxyConfig proxy,
      final File downloadDir, final File mediaDir, final boolean saveMedia) {
    mySettingsId = -1;
    this.requestHeaders = requestHeaders == null ? new RequestHeaders() : requestHeaders;
    this.browserTimeZone = browserTimeZone == null ? BrowserTimeZone.UTC : browserTimeZone;
    this.browserProperties = browserProperties == null ? new BrowserProperties() : browserProperties;
    this.proxy = proxy == null ? new ProxyConfig() : proxy;
    this.downloadDir = downloadDir == null ? new File("./download_cache") : downloadDir;
    if (!this.downloadDir.exists()) {
      this.downloadDir.mkdirs();
      this.downloadDir.deleteOnExit();
    }
    this.mediaDir = mediaDir == null ? new File("./media_cache") : mediaDir;
    if (!this.mediaDir.exists()) {
      this.mediaDir.mkdirs();
      this.mediaDir.deleteOnExit();
    }
    this.saveMedia = saveMedia;

    StringBuilder scriptBuilder = new StringBuilder();
    String scriptId = "A" + rand.nextLong();
    scriptBuilder.append("<script id='" + scriptId + "' language='javascript'>");
    scriptBuilder.append("try{");
    scriptBuilder.append(browserTimeZone().script());
    scriptBuilder.append(browserProperties().script());
    scriptBuilder.append("}catch(e){}");
    scriptBuilder.append("document.getElementsByTagName('head')[0].removeChild("
        + "document.getElementById('" + scriptId + "'));");
    scriptBuilder.append("</script>");
    script = scriptBuilder.toString();
  }

  Settings(Settings original) {
    requestHeaders = original.requestHeaders;
    browserTimeZone = original.browserTimeZone;
    browserProperties = original.browserProperties;
    proxy = original.proxy;
    downloadDir = original.downloadDir;
    mediaDir = original.mediaDir;
    saveMedia = original.saveMedia;
    mySettingsId = settingsId.incrementAndGet();
    script = original.script;
  }

  long id() {
    return mySettingsId;
  }

  RequestHeaders headers() {
    return requestHeaders;
  }

  BrowserTimeZone browserTimeZone() {
    return browserTimeZone;
  }

  BrowserProperties browserProperties() {
    return browserProperties;
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

  CookieManager cookieManager() {
    return cookieManager;
  }

  static boolean headless() {
    return headless;
  }
}
