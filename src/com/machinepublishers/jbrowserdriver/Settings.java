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
package com.machinepublishers.jbrowserdriver;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.machinepublishers.jbrowserdriver.StreamInjectors.Injector;

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

  static {
    final Pattern head = Pattern.compile("<head\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Pattern html = Pattern.compile("<html\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    final Pattern body = Pattern.compile("<body\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    StreamInjectors.add(new Injector() {
      @Override
      public byte[] inject(StreamConnection connection,
          byte[] inflatedContent, String originalUrl) {
        final Settings settings = SettingsManager.settings();
        try {
          if (settings.saveMedia()
              && connection.isMedia()) {
            String filename = Long.toString(System.nanoTime());
            File contentFile = new File(StreamConnection.mediaDir(), filename + ".content");
            File metaFile = new File(StreamConnection.mediaDir(), filename + ".metadata");
            while (contentFile.exists() || metaFile.exists()) {
              filename = Long.toString(Math.abs(rand.nextLong()));
              contentFile = new File(StreamConnection.mediaDir(), filename + ".content");
              metaFile = new File(StreamConnection.mediaDir(), filename + ".metadata");
            }
            contentFile.deleteOnExit();
            metaFile.deleteOnExit();
            Files.write(contentFile.toPath(), inflatedContent);
            Files.write(metaFile.toPath(),
                (originalUrl + "\n" + connection.getContentType()).getBytes("utf-8"));
          }
        } catch (Throwable t) {}
        try {
          if (settings.quickRender() && connection.isMedia()) {
            LogsServer.instance().trace("Media discarded: " + connection.getURL().toExternalForm());
            StatusMonitor.instance().addDiscarded(connection.getURL().toExternalForm());
            return new byte[0];
          } else if ((connection.getContentType() == null || connection.getContentType().indexOf("text/html") > -1)
              && StatusMonitor.instance().isPrimaryDocument(connection.getURL().toExternalForm())) {
            String injected = null;
            String charset = Util.charset(connection);
            String content = new String(inflatedContent, charset);
            Matcher matcher = head.matcher(content);
            if (matcher.find()) {
              injected = matcher.replaceFirst(matcher.group(0) + settings.script());
            } else {
              matcher = html.matcher(content);
              if (matcher.find()) {
                injected = matcher.replaceFirst(
                    matcher.group(0) + "<head>" + settings.script() + "</head>");
              } else {
                matcher = body.matcher(content);
                if (matcher.find()) {
                  injected = ("<html><head>" + settings.script() + "</head>"
                      + content + "</html>");
                } else {
                  injected = content;
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
    private org.openqa.selenium.Dimension screen = new org.openqa.selenium.Dimension(1000, 600);
    private UserAgent userAgent = UserAgent.TOR;
    private Timezone timezone = Timezone.UTC;
    private String headScript = HEAD_SCRIPT;
    private ProxyConfig proxy = new ProxyConfig();
    private boolean saveMedia;
    private boolean saveAttachments;
    private boolean ignoreDialogs = true;
    private boolean cache;
    private File cacheDir;
    private int cacheEntries = 10 * 1000;
    private long cacheEntrySize = 1000 * 1000;
    private final Collection<Integer> ports = new LinkedHashSet<Integer>();
    private boolean headless = true;
    private long ajaxWait = 120;
    private long ajaxResourceTimeout = 2000;
    private boolean blockAds = true;
    private boolean quickRender = true;
    private int maxRouteConnections = 8;
    private int maxConnections = 3000;
    private String ssl;
    private boolean traceConsole;
    private boolean warnConsole = true;
    private boolean wireConsole;
    private int maxLogs = 5000;

    public Builder() {
      for (int i = 10000; i < 10008; i++) {
        ports.add(i);
      }
    }

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
    public Builder screen(org.openqa.selenium.Dimension screen) {
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
     * Whether to save media (e.g., images) to disk. Defaults to <code>false</code>.
     * <p>
     * Media is saved as two files in the mediaDir: &lt;identifier&gt;.content (which
     * has the binary content) and &lt;identifier&gt;.metadata (the first line of this
     * file is the original URL of the request for this media, and the second line is
     * the mime type).
     * <p>
     * Saved media files are deleted when the JVM exits, so copy them to a different
     * location to persist them permanently.
     * <p>
     * The temporary directory where these files are saved is availble from
     * {@link JBrowserDriver#mediaDir()}
     * 
     * @param saveMedia
     *          Whether to save media (e.g., images) to disk.
     * @return this Builder
     */
    public Builder saveMedia(boolean saveMedia) {
      this.saveMedia = saveMedia;
      return this;
    }

    /**
     * Whether to save links to disk when prompted by the browser. Defaults to <code>false</code>.
     * <p>
     * Saved files are deleted when the JVM exits, so copy them to a different
     * location to persist them permanently.
     * <p>
     * The temporary directory where these files are saved is availble from
     * {@link JBrowserDriver#attachmentsDir()}
     * 
     * @param saveAttachments
     * @return
     */
    public Builder saveAttachments(boolean saveAttachments) {
      this.saveAttachments = saveAttachments;
      return this;
    }

    /**
     * Whether JavaScript alerts, prompts, and confirm dialogs should be auto-dismissed
     * and ignored. Otherwise, you will need to use JBrowserDriver.switchTo().alert()
     * to accept/dismiss these dialogs. Note that if dialogs are not ignored and you are handling them,
     * that calls to alert.accept(), alert.dismiss(), and alert.sendKeys(String) are queued,
     * so there is no need to wait for the dialog to actually be displayed and these calls will not block.
     * Calls to alert.getText() block until an alert is shown (unless a script timeout is reached first).
     * 
     * @param ignoreDialogs
     *          <code>True</code> to auto-dismiss alert/prompt/confirm dialogs and relieve the
     *          user from having to handle them. <code>False</code> to allow these dialogs to have an effect on the page
     *          and force the user to accept/dismiss them. Defaults to <code>true</code>.
     * @return this Builder
     */
    public Builder ignoreDialogs(boolean ignoreDialogs) {
      this.ignoreDialogs = ignoreDialogs;
      return this;
    }

    /**
     * Whether to cache web pages like a desktop browser would. Defaults to <code>false</code>.
     * <p>
     * The temporary directory where these files are saved is availble from
     * {@link JBrowserDriver#cacheDir()}
     * 
     * @param cache
     * @return this Builder
     */
    public Builder cache(boolean cache) {
      this.cache = cache;
      return this;
    }

    /**
     * Directory where the web cache resides. This enables sharing a cache across instances
     * and after JVM restarts.
     * 
     * @param cacheDir
     *          cache directory
     * @return this Builder
     */
    public Builder cacheDir(File cacheDir) {
      this.cacheDir = cacheDir;
      return this;
    }

    /**
     * Set maximum number of cached files on disk. Defaults to 10000.
     * 
     * @param cacheEntries
     * @return this Builder
     */
    public Builder cacheEntries(int cacheEntries) {
      this.cacheEntries = cacheEntries;
      return this;
    }

    /**
     * Set maximum size of a file to be cached. If it's greater than this max, it will not be cached.
     * Defaults to 1 MB.
     * 
     * @param bytes
     * @return this Builder
     */
    public Builder cacheEntrySize(long bytes) {
      this.cacheEntrySize = bytes;
      return this;
    }

    /**
     * Each browser instance is run in a separate process (via RMI).
     * This setting configures which ports are available for RMI.
     * The number of ports determines the maximum number of RMI processes.
     * 
     * Defaults to <code>10000,10001,10002,10003,10004,10005,10006,10007</code>
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.ports</code>
     * 
     * @param ports
     * @return this Builder
     */
    public Builder ports(int... ports) {
      this.ports.clear();
      for (int i = 0; ports != null && i < ports.length; i++) {
        this.ports.add(ports[i]);
      }
      return this;
    }

    /**
     * Each browser instance is run in a separate process (via RMI).
     * This setting configures which ports are available for RMI.
     * The number of ports determines the maximum number of RMI processes.
     * This is a convenience method for those who are allocating a sequential
     * range of ports, as you only need to specify the starting port (inclusive) and
     * maximum number of ports/processes.
     * 
     * Defaults to <code>10000,8</code>
     * 
     * @param startingPort
     * @param maxProcesses
     * @return
     */
    public Builder portsMax(int startingPort, int maxProcesses) {
      this.ports.clear();
      for (int i = 0; i < maxProcesses; i++) {
        this.ports.add(startingPort + i);
      }
      return this;
    }

    /**
     * Whether to run in headless mode (no GUI windows).
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.headless</code>
     * 
     * @param headless
     * @return this Builder
     */
    public Builder headless(boolean headless) {
      this.headless = headless;
      return this;
    }

    /**
     * The idle time (no pending AJAX requests) required in milliseconds before a page is considered to
     * have been loaded completely. For very slow or overloaded CPUs, set a higher value.
     * Defaults to <code>120</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.ajaxwait</code>
     * 
     * @param intervalMS
     * @return this Builder
     */
    public Builder ajaxWait(long intervalMS) {
      this.ajaxWait = intervalMS;
      return this;
    }

    /**
     * The time in milliseconds after which an AJAX request will be ignored when considering
     * whether all AJAX requests have completed. Defaults to <code>2000</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.ajaxresourcetimeout</code>
     * 
     * @param timeoutMS
     * @return this Builder
     */
    public Builder ajaxResourceTimeout(long timeoutMS) {
      this.ajaxResourceTimeout = timeoutMS;
      return this;
    }

    /**
     * Whether requests to ad/spam servers should be blocked.
     * Based on hosts in ad-hosts.txt in the source tree. Defaults to <code>true</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.blockads</code>
     * 
     * @param blockAds
     * @return this Builder
     */
    public Builder blockAds(boolean blockAds) {
      this.blockAds = blockAds;
      return this;
    }

    /**
     * Exclude web page images and binary data from rendering.
     * These resources are still requested and can optionally be saved to disk (see the Settings options).
     * Some versions of Java are inefficient (memory-wise) in rendering images.
     * Defaults to <code>true</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.quickrender</code>
     * 
     * @param quickRender
     * @return this Builder
     */
    public Builder quickRender(boolean quickRender) {
      this.quickRender = quickRender;
      return this;
    }

    /**
     * Maximum number of concurrent connections to a specific host+proxy combo. Defaults to <code>8</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.maxrouteconnections</code>
     * 
     * @param maxRouteConnections
     * @return this Builder
     */
    public Builder maxRouteConnections(int maxRouteConnections) {
      this.maxRouteConnections = maxRouteConnections;
      return this;
    }

    /**
     * Maximum number of concurrent connections overall. Defaults to <code>3000</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.maxconnections</code>
     * 
     * @param maxConnections
     * @return this Builder
     */
    public Builder maxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
      return this;
    }

    /**
     * Specifies a source of trusted certificate authorities.
     * Can take one of four values:
     * <br>(1) <code>compatible</code> to accept standard browser certs,
     * <br>(2) <code>trustanything</code> to accept any SSL cert,
     * <br>(3) a file path, or
     * <br>(4) a URL.
     * <br>If a file or URL is specified it must follow exactly the format of content like this: <a
     * href="https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt">https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt</a>
     * <br>The default when this property is not set (i.e., when it's <code>null</code>) is your JRE's keystore,
     * so you can use JDK's keytool to import specific certs.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.ssl</code>
     * 
     * @param ssl
     * @return this Builder
     */
    public Builder ssl(String ssl) {
      this.ssl = ssl;
      return this;
    }

    /**
     * Mirror trace-level log messages to standard out.
     * Otherwise these logs are only available through the Selenium APIs. Defaults to <code>false</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.traceconsole</code>
     * 
     * @param traceConsole
     * @return this Builder
     */
    public Builder traceConsole(boolean traceConsole) {
      this.traceConsole = traceConsole;
      return this;
    }

    /**
     * Mirror warning-level log messages to standard error.
     * Otherwise these logs are only available through the Selenium APIs. Defaults to <code>true</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.warnconsole</code>
     * 
     * @param warnConsole
     * @return this Builder
     */
    public Builder warnConsole(boolean warnConsole) {
      this.warnConsole = warnConsole;
      return this;
    }

    /**
     * Log full requests and responses (except response bodies) to standard out.
     * This produces an enormous amount of output and logs potentially sensitive data--use only as needed.
     * Regardless of this setting, these log messages are never available via the Selenium logging APIs.
     * Defaults to <code>false</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.wireconsole</code>
     * 
     * @param wireConsole
     * @return this Builder
     */
    public Builder wireConsole(boolean wireConsole) {
      this.wireConsole = wireConsole;
      return this;
    }

    /**
     * Maximum number of log entries to store in memory, accessible via the Selenium APIs.
     * The oldest log entry is dropped once the max is reached. Regardless of this setting,
     * logs are cleared per instance of JBrowserDriver after a call to quit(), reset(), or Logs.get(String).
     * Defaults to <code>5000</code>.
     * 
     * <p>This setting can be overridden with Java system property <code>jbd.maxlogs</code>
     * 
     * @param maxLogs
     * @return this Builder
     */
    public Builder maxLogs(int maxLogs) {
      this.maxLogs = maxLogs;
      return this;
    }

    /**
     * @return A Settings object created from this builder.
     * @see JBrowserDriver#JBrowserDriver(Settings)
     */
    public Settings build() {
      return new Settings(this);
    }
  }

  private final RequestHeaders requestHeaders;
  private final int screenWidth;
  private final int screenHeight;
  private final String userAgentString;
  private final ProxyConfig proxy;
  private final boolean saveMedia;
  private final boolean saveAttachments;
  private final String script;
  private final boolean ignoreDialogs;
  private final boolean cache;
  private final File cacheDir;
  private final int cacheEntries;
  private final long cacheEntrySize;
  private final Collection<Integer> ports;
  private final boolean headless;
  private final long ajaxWait;
  private final long ajaxResourceTimeout;
  private final boolean blockAds;
  private final boolean quickRender;
  private final int maxRouteConnections;
  private final int maxConnections;
  private final String ssl;
  private final boolean traceConsole;
  private final boolean warnConsole;
  private final boolean wireConsole;
  private final int maxLogs;

  private Settings(Settings.Builder builder) {
    this.requestHeaders = builder.requestHeaders;
    this.screenWidth = builder.screen.getWidth();
    this.screenHeight = builder.screen.getHeight();
    this.userAgentString = builder.userAgent.userAgentString();
    this.proxy = builder.proxy;
    this.saveMedia = builder.saveMedia;
    this.saveAttachments = builder.saveAttachments;
    this.ignoreDialogs = builder.ignoreDialogs;
    this.cache = builder.cache;
    this.cacheDir = builder.cacheDir;
    this.cacheEntries = builder.cacheEntries;
    this.cacheEntrySize = builder.cacheEntrySize;

    if (System.getProperty("jbd.ports") == null) {
      this.ports = builder.ports;
    } else {
      this.ports = new LinkedHashSet<Integer>();
      String[] ranges = System.getProperty("jbd.ports").split(",");
      for (int i = 0; i < ranges.length; i++) {
        String[] bounds = ranges[i].split("-");
        int low = Integer.parseInt(bounds[0]);
        int high = bounds.length > 1 ? Integer.parseInt(bounds[1]) : low;
        for (int j = low; j <= high; j++) {
          this.ports.add(j);
        }
      }
    }
    if (System.getProperty("jbd.headless") == null) {
      //backwards compatible property name for versions <= 0.9.1
      this.headless = System.getProperty("jbd.browsergui") == null
          ? builder.headless : !Boolean.parseBoolean(System.getProperty("jbd.browsergui"));
    } else {
      this.headless = Boolean.parseBoolean(System.getProperty("jbd.headless"));
    }

    this.ajaxWait = System.getProperty("jbd.ajaxwait") == null
        ? builder.ajaxWait : Long.parseLong(System.getProperty("jbd.ajaxwait"));

    this.ajaxResourceTimeout = System.getProperty("jbd.ajaxresourcetimeout") == null
        ? builder.ajaxResourceTimeout : Long.parseLong(System.getProperty("jbd.ajaxresourcetimeout"));

    this.blockAds = System.getProperty("jbd.blockads") == null
        ? builder.blockAds : Boolean.parseBoolean(System.getProperty("jbd.blockads"));

    this.quickRender = System.getProperty("jbd.quickrender") == null
        ? builder.quickRender : Boolean.parseBoolean(System.getProperty("jbd.quickrender"));

    this.maxRouteConnections = System.getProperty("jbd.maxrouteconnections") == null
        ? builder.maxRouteConnections : Integer.parseInt(System.getProperty("jbd.maxrouteconnections"));

    this.maxConnections = System.getProperty("jbd.maxconnections") == null
        ? builder.maxConnections : Integer.parseInt(System.getProperty("jbd.maxconnections"));

    if (System.getProperty("jbd.ssl") == null) {
      //backwards compatible property name for versions <= 0.9.1
      this.ssl = System.getProperty("jbd.pemfile") == null
          ? builder.ssl : System.getProperty("jbd.pemfile");
    } else {
      this.ssl = System.getProperty("jbd.ssl");
    }

    this.traceConsole = System.getProperty("jbd.traceconsole") == null
        ? builder.traceConsole : Boolean.parseBoolean(System.getProperty("jbd.traceconsole"));

    this.warnConsole = System.getProperty("jbd.warnconsole") == null
        ? builder.warnConsole : Boolean.parseBoolean(System.getProperty("jbd.warnconsole"));

    this.wireConsole = System.getProperty("jbd.wireconsole") == null
        ? builder.wireConsole : Boolean.parseBoolean(System.getProperty("jbd.wireconsole"));

    this.maxLogs = System.getProperty("jbd.maxlogs") == null
        ? builder.maxLogs : Integer.parseInt(System.getProperty("jbd.maxlogs"));

    StringBuilder scriptBuilder = new StringBuilder();
    String scriptId = "A" + rand.nextLong();
    if (headless()) {
      scriptBuilder.append("<style>body::-webkit-scrollbar {width: 0px !important;height:0px !important;}</style>");
    }
    scriptBuilder.append("<script id='" + scriptId + "' language='javascript'>");
    scriptBuilder.append("try{");
    scriptBuilder.append(builder.userAgent.script());
    scriptBuilder.append(builder.timezone.script());
    if (builder.headScript != null) {
      scriptBuilder.append(builder.headScript);
    }
    scriptBuilder.append("}catch(e){}");
    scriptBuilder.append("document.getElementsByTagName('head')[0].removeChild(document.getElementById('" + scriptId + "'));");
    scriptBuilder.append("</script>");
    this.script = scriptBuilder.toString();
  }

  RequestHeaders headers() {
    return requestHeaders;
  }

  int screenWidth() {
    return screenWidth;
  }

  int screenHeight() {
    return screenHeight;
  }

  String userAgentString() {
    return userAgentString;
  }

  ProxyConfig proxy() {
    return proxy;
  }

  boolean saveMedia() {
    return saveMedia;
  }

  boolean saveAttachments() {
    return saveAttachments;
  }

  String script() {
    return script;
  }

  boolean ignoreDialogs() {
    return ignoreDialogs;
  }

  boolean cache() {
    return cache;
  }

  File cacheDir() {
    return cacheDir;
  }

  int cacheEntries() {
    return cacheEntries;
  }

  long cacheEntrySize() {
    return cacheEntrySize;
  }

  Collection<Integer> ports() {
    return ports;
  }

  boolean headless() {
    return headless;
  }

  long ajaxWait() {
    return ajaxWait;
  }

  long ajaxResourceTimeout() {
    return ajaxResourceTimeout;
  }

  boolean blockAds() {
    return blockAds;
  }

  boolean quickRender() {
    return quickRender;
  }

  int maxRouteConnections() {
    return maxRouteConnections;
  }

  int maxConnections() {
    return maxConnections;
  }

  String ssl() {
    return ssl;
  }

  boolean traceConsole() {
    return traceConsole;
  }

  boolean warnConsole() {
    return warnConsole;
  }

  boolean wireConsole() {
    return wireConsole;
  }

  int maxLogs() {
    return maxLogs;
  }
}
