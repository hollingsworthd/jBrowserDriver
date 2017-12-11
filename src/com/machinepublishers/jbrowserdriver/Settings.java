/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
 * https://github.com/MachinePublishers/jBrowserDriver
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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * An immutable class which contains settings for the browser.
 *
 * @see Settings#builder()
 * @see JBrowserDriver#JBrowserDriver(Settings)
 */
public class Settings implements Serializable {
  private static final Logger defaultLogger;
  static {
    if (LogManager.getLogManager().getLogger("com.machinepublishers.jbrowserdriver") == null) {
      defaultLogger = Logger.getLogger("com.machinepublishers.jbrowserdriver");
      defaultLogger.setUseParentHandlers(false);
      defaultLogger.addHandler(new LogHandler());
      defaultLogger.setLevel(Level.ALL);
    } else {
      defaultLogger = LogManager.getLogManager().getLogger("com.machinepublishers.jbrowserdriver");
    }
  }
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
    builder.append("'getImageData', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, ");
    builder.append("'measureText', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, ");
    builder.append("'isPointInPath', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, ");
    builder.append("'isPointInStroke', {value:function(){return undefined;}});");
    HEAD_SCRIPT = builder.toString();
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

  private enum PropertyName {
    @Deprecated
    PORTS("jbd.ports"),
    PORT_RANGES("jbd.portranges"),
    PROCESSES("jbd.processes"),
    HOST("jbd.host"),
    HEADLESS("jbd.headless"),
    AJAX_WAIT("jbd.ajaxwait"),
    AJAX_RESOURCE_TIMEOUT("jbd.ajaxresourcetimeout"),
    BLOCK_ADS("jbd.blockads"),
    QUICK_RENDER("jbd.quickrender"),
    MAX_ROUTE_CONNECTIONS("jbd.maxrouteconnections"),
    MAX_CONNECTIONS("jbd.maxconnections"),
    SSL("jbd.ssl"),
    USER_AGENT("jbd.useragent"),
    SCREEN_WIDTH("jbd.screenwidth"),
    SCREEN_HEIGHT("jbd.screenheight"),
    TIMEZONE("jbd.timezone"),
    HEAD_SCRIPT("jbd.headscript"),
    PROXY_HOST("jbd.proxyhost"),
    PROXY_PORT("jbd.proxyport"),
    PROXY_TYPE("jbd.proxytype"),
    PROXY_USERNAME("jbd.proxyusername"),
    PROXY_PASSWORD("jbd.proxypassword"),
    PROXY_EXPECT_CONTINUE("jbd.proxyexpectcontinue"),
    SAVE_MEDIA("jbd.savemedia"),
    SAVE_ATTACHMENTS("jbd.saveattachments"),
    IGNORE_DIALOGS("jbd.ignoredialogs"),
    CACHE("jbd.cache"),
    CACHE_DIR("jbd.cachedir"),
    CACHE_ENTRIES("jbd.cacheentries"),
    CACHE_ENTRY_SIZE("jbd.cacheentrysize"),
    HOSTNAME_VERIFICATION("jbd.hostnameverification"),
    JAVASCRIPT("jbd.javascript"),
    SOCKET_TIMEOUT_MS("jbd.sockettimeout"),
    CONNECT_TIMEOUT_MS("jbd.connecttimeout"),
    CONNECTION_REQ_TIMEOUT_MS("jbd.connectionreqtimeout"),
    RESPONSE_INTERCEPTORS("jbd.responseinterceptors"),
    LOG_WIRE("jbd.logwire"),
    LOG_JAVASCRIPT("jbd.logjavascript"),
    LOG_TRACE("jbd.logtrace"),
    LOG_WARNINGS("jbd.logwarnings"),
    LOGS_MAX("jbd.logsmax"),
    LOGGER("jbd.logger"),
    JAVA_OPTIONS("jbd.javaoptions"),
    JAVA_BINARY("jbd.javabinary"),
    JAVA_EXPORT_MODULES("jbd.javaexportmodules"),
    USER_DATA_DIRECTORY("jbd.userdatadirectory"),
    CSRF_REQUEST_TOKEN("jbd.csrfreqtoken"),
    CSRF_RESPONSE_TOKEN("jbd.csrfresptoken"),
    @Deprecated
    WIRE_CONSOLE("jbd.wireconsole"),
    @Deprecated
    TRACE_CONSOLE("jbd.traceconsole"),
    @Deprecated
    WARN_CONSOLE("jbd.warnconsole"),
    @Deprecated
    MAX_LOGS("jbd.maxlogs");

    private final String propertyName;

    PropertyName(String propertyName) {
      this.propertyName = propertyName;
    }
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
    private String portRanges;
    private int processes = 2 * Runtime.getRuntime().availableProcessors();
    private boolean headless = true;
    private long ajaxWait = 150;
    private long ajaxResourceTimeout = 2000;
    private boolean blockAds;
    private boolean quickRender;
    private int maxRouteConnections = 8;
    private int maxConnections = 300;
    private String ssl;
    private boolean logWire;
    private boolean logJavascript;
    private boolean logTrace;
    private boolean logWarnings = true;
    private Logger logger = defaultLogger;
    private int logsMax = 1000;
    private boolean hostnameVerification = true;
    private boolean javascript = true;
    private int socketTimeout = -1;
    private int connectTimeout = -1;
    private int connectionReqTimeout = -1;
    private String host = "127.0.0.1";
    private String[] javaOptions;
    private String javaBinary;
    private boolean javaExportModules;
    private File userDataDirectory;
    private String csrfRequestToken;
    private String csrfResponseToken;
    private InetAddress nicAddress;

    /**
     * Headers to be sent on each request.
     * <p>
     * Defaults to {@link RequestHeaders#TOR}.
     * <p>
     * (Not configurable via Java system properties or {@link Capabilities}. See {@link Settings.Builder#userAgent(UserAgent)} instead.)
     *
     * @param requestHeaders
     *
     * @return this Builder
     * @see Builder#userAgent(UserAgent)
     */
    public Builder requestHeaders(RequestHeaders requestHeaders) {
      this.requestHeaders = requestHeaders;
      return this;
    }

    /**
     * Size of the screen and initial size of the window.
     * <p>
     * Defaults to 1000x600. This is a typical size for Tor Browser but if you're not using the
     * Tor user agent you might want to specify a more common size such as 1366x768.
     *
     * <p><ul>
     * <li>Java system properties <code>jbd.screenwidth</code> and <code>jbd.screenheight</code> override this setting.</li>
     * <li>{@link Capabilities} names <code>jbd.screenwidth</code> and <code>jbd.screenheight</code> alternately configure this setting.</li>
     * </ul><p>
     *
     * @param screen
     *          Screen and window size.
     *
     * @return this Builder
     */
    public Builder screen(org.openqa.selenium.Dimension screen) {
      this.screen = screen;
      return this;
    }

    /**
     * User agent and window.navigator properties.
     * <p>
     * Defaults to {@link UserAgent#TOR}.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.useragent</code> overrides this setting and the {@link Builder#requestHeaders(RequestHeaders)} setting. The value must be one of: tor, chrome.</li>
     * <li>{@link Capabilities} name <code>jbd.useragent</code> alternately configures this setting and the {@link Builder#requestHeaders(RequestHeaders)} setting. The value must be one of: tor,
     * chrome.</li>
     * </ul><p>
     *
     * @param userAgent
     *          Browser's user agent
     *
     * @return this Builder
     * @see Builder#requestHeaders(RequestHeaders)
     */
    public Builder userAgent(UserAgent userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    /**
     * Timezone of the browser.
     * <p>
     * Defaults to {@link Timezone#UTC}.
     * This is the timezone of Tor Browser but if you're not using the Tor user agent
     * you might want to use a locale nearer your actual computer,
     * such as {@link Timezone#AMERICA_LOSANGELES} if you're in San Francisco.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.timezone</code> overrides this setting. For value, see {@link Timezone#name()}.</li>
     * <li>{@link Capabilities} name <code>jbd.timezone</code> alternately configures this setting. For value, see {@link Timezone#name()}.</li>
     * </ul><p>
     *
     * @param timezone
     *          Timezone of the browser.
     *
     * @return this Builder
     */
    public Builder timezone(Timezone timezone) {
      this.timezone = timezone;
      return this;
    }

    /**
     * Script to be injected in the HTML Head section.
     * <p>
     * Omit &lt;script&gt; tags; they will be added automatically.
     * <p>
     * Defaults to {@link Settings#HEAD_SCRIPT}.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.headscript</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.headscript</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param headScript
     *          Script to be injected
     *
     * @return this Builder
     */
    public Builder headScript(String headScript) {
      this.headScript = headScript;
      return this;
    }

    /**
     * Proxy server to be used.
     * <p>
     * Defaults to a direct connection (no proxy).
     *
     * <p><ul>
     * <li>Java system properties <code>jbd.proxytype</code>, <code>jbd.proxyhost</code>, <code>jbd.proxyport</code>, <code>jbd.proxyusername</code>, <code>jbd.proxypassword</code>, and
     * <code>jbd.proxyexpectcontinue</code> override this setting.</li>
     * <li>{@link Capabilities} names <code>jbd.proxytype</code>, <code>jbd.proxyhost</code>, <code>jbd.proxyport</code>, <code>jbd.proxyusername</code>, <code>jbd.proxypassword</code>, and
     * <code>jbd.proxyexpectcontinue</code> alternately configure this setting.</li>
     * </ul><p>
     *
     * @param proxy
     *          Proxy configuration or <code>null</code>.
     *
     * @return this Builder
     */
    public Builder proxy(ProxyConfig proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Whether to save media (e.g., images) to disk.
     * <p>
     * Media is saved as two files in the mediaDir: &lt;identifier&gt;.content (which
     * has the binary content) and &lt;identifier&gt;.metadata (the first line of this
     * file is the URL of the request for this media, the second line is
     * the Content-Type header, and the third line is the Content-Disposition header).
     * <p>
     * Saved media files are deleted when the JVM exits, so copy them to a different
     * location to persist them permanently.
     * <p>
     * The temporary directory where these files are saved is availble from
     * {@link JBrowserDriver#mediaDir()}
     * <p>
     * Defaults to <code>false</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.savemedia</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.savemedia</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param saveMedia
     *          Whether to save media (e.g., images) to disk.
     *
     * @return this Builder
     */
    public Builder saveMedia(boolean saveMedia) {
      this.saveMedia = saveMedia;
      return this;
    }

    /**
     * Whether to save links to disk when prompted by the browser.
     * <p>
     * Attachments are saved as two files in the mediaDir: &lt;identifier&gt;.content (which
     * has the binary content) and &lt;identifier&gt;.metadata (the first line of this
     * file is the URL of the request for this media, the second line is
     * the Content-Type header, and the third line is the Content-Disposition header).
     * <p>
     * Saved files are deleted when the JVM exits, so copy them to a different
     * location to persist them permanently.
     * <p>
     * The temporary directory where these files are saved is availble from
     * {@link JBrowserDriver#attachmentsDir()}
     * <p>
     * Defaults to <code>false</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.saveattachments</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.saveattachments</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param saveAttachments
     *
     * @return this Builder
     */
    public Builder saveAttachments(boolean saveAttachments) {
      this.saveAttachments = saveAttachments;
      return this;
    }

    /**
     * Whether JavaScript alerts, prompts, and confirm dialogs should be auto-dismissed
     * and ignored. Otherwise, you will need to use JBrowserDriver.switchTo().alert()
     * to accept/dismiss these dialogs.
     * <p>
     * Note that if dialogs are not ignored and you are handling them,
     * that calls to alert.accept(), alert.dismiss(), and alert.sendKeys(String) are queued,
     * so there is no need to wait for the dialog to actually be displayed and these calls will not block.
     * Calls to alert.getText() block until an alert is shown (unless a script timeout is reached first).
     * <p>
     * Defaults to <code>true</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.ignoredialogs</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.ignoredialogs</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param ignoreDialogs
     *          <code>True</code> to auto-dismiss alert/prompt/confirm dialogs and relieve the
     *          user from having to handle them. <code>False</code> to allow these dialogs to have an effect on the page
     *          and force the user to accept/dismiss them.
     *
     * @return this Builder
     */
    public Builder ignoreDialogs(boolean ignoreDialogs) {
      this.ignoreDialogs = ignoreDialogs;
      return this;
    }

    /**
     * Whether to cache web pages like a desktop browser would.
     * <p>
     * The temporary directory where these files are saved is availble from
     * {@link JBrowserDriver#cacheDir()}
     * <p>
     * Defaults to <code>false</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.cache</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.cache</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param cache
     *
     * @return this Builder
     */
    public Builder cache(boolean cache) {
      this.cache = cache;
      return this;
    }

    /**
     * Directory where the web cache resides--this enables sharing a cache across instances and after JVM restarts.
     * <p>
     * Defaults to <code>null</code> meaning a temp directory will be generated, available via {@link JBrowserDriver#cacheDir()}.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.cachedir</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.cachedir</code> alternately configures this setting.</li>
     * </ul><p>
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
     * Set maximum number of cached files on disk.
     * <p>
     * Defaults to 10000.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.cacheentries</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.cacheentries</code> alternately configures this setting.</li>
     * </ul><p>
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
     * <p>
     * Defaults to 1 MB.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.cacheentrysize</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.cacheentrysize</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param bytes
     * @return this Builder
     */
    public Builder cacheEntrySize(long bytes) {
      this.cacheEntrySize = bytes;
      return this;
    }

    /**
     * @deprecated Will be removed in v2.0.0. Use Settings.Builder.processes(..) instead.
     */
    @Deprecated
    public Builder ports(int... ports) {
      System.err.println("jBrowserDriver: The ports setting is deprecated and will be removed in v2.0.0. Use Settings.Builder.processes(..) instead.");
      this.portRanges = null;
      this.processes = ports.length;
      this.host = "127.0.0.1";
      return this;
    }

    /**
     * @deprecated Will be removed in v2.0.0. Use Settings.Builder.processes(..) instead.
     */
    @Deprecated
    public Builder portsMax(int startingPort, int maxProcesses) {
      System.err.println("jBrowserDriver: The portsMax setting is deprecated and will be removed in v2.0.0. Use Settings.Builder.processes(..) instead.");
      this.portRanges = null;
      this.processes = maxProcesses;
      this.host = "127.0.0.1";
      return this;
    }

    /**
     * The number of {@link JBrowserDriver} instances that can run concurrently, using any available port.
     * <p>
     * Each instance of JBrowserDriver is backed by a separate Java process operated via RMI.
     * <p>
     * Overwrites settings specified by any previous calls to processes(..)
     * <p>
     * By default any available ports are used, the host is <code>127.0.0.1</code>, and the max number of concurrent
     * instances is <code>2 * Runtime.getRuntime().availableProcessors()</code>
     *
     * <p><ul>
     * <li>Java system property <code>jbd.processes</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.processes</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param maxProcesses
     * @return this Builder
     */
    public Builder processes(int maxProcesses) {
      this.portRanges = null;
      this.processes = maxProcesses;
      this.host = "127.0.0.1";
      return this;
    }

    /**
     * The number of {@link JBrowserDriver} instances that can run concurrently,
     * using any available port, and the host name or IP of the local machine.
     * <p>
     * Each instance of JBrowserDriver is backed by a separate Java process operated via RMI.
     * <p>
     * Overwrites settings specified by any previous calls to processes(..)
     * <p>
     * By default any available ports are used, the host is <code>127.0.0.1</code>, and the max number of concurrent
     * instances is <code>2 * Runtime.getRuntime().availableProcessors()</code>
     *
     * <p><ul>
     * <li>Java system properties <code>jbd.processes</code> and <code>jbd.host</code> override this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.processes</code> and <code>jbd.host</code> alternately configure this setting.</li>
     * </ul><p>
     *
     * @param maxProcesses
     * @param host
     * @return this Builder
     */
    public Builder processes(int maxProcesses, String host) {
      this.portRanges = null;
      this.processes = maxProcesses;
      this.host = host;
      return this;
    }

    /**
     * The ports used by {@link JBrowserDriver} instances and the parent process.
     * <p>
     * The max number of instances that can run concurrently is inferred from the number of ports provided
     * (every instance requires three ports -- e.g., for up to 8 instances you need 24 ports).
     * <p>
     * Each instance of JBrowserDriver is backed by a separate Java process operated via RMI.
     * <p>
     * Overwrites settings specified by any previous calls to processes(..)
     * <p>
     * By default any available ports are used, the host is <code>127.0.0.1</code>, and the max number of concurrent
     * instances is <code>2 * Runtime.getRuntime().availableProcessors()</code>
     *
     * <p><ul>
     * <li>Java system property <code>jbd.portranges</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.portranges</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param portRanges
     *          A comma separated list of ports and/or port ranges
     *          (ranges are inclusive and separated by a dash) -- e.g., <code>10000-10007,12500,12502,15376-15380</code>
     * @return this Builder
     */
    public Builder processes(String portRanges) {
      this.portRanges = portRanges;
      this.processes = -1;
      this.host = "127.0.0.1";
      return this;
    }

    /**
     * The ports and host/IP used by {@link JBrowserDriver} instances and the parent process.
     * <p>
     * The max number of instances that can run concurrently is inferred from the number of ports provided
     * (every instance requires three ports -- e.g., for up to 8 instances you need 24 ports).
     * <p>
     * Each instance of JBrowserDriver is backed by a separate Java process operated via RMI.
     * <p>
     * Overwrites settings specified by any previous calls to processes(..)
     * <p>
     * By default any available ports are used, the host is <code>127.0.0.1</code>, and the max number of concurrent
     * instances is <code>2 * Runtime.getRuntime().availableProcessors()</code>
     *
     * <p><ul>
     * <li>Java system properties <code>jbd.portranges</code> and <code>jbd.host</code> override this setting.</li>
     * <li>{@link Capabilities} names <code>jbd.portranges</code> and <code>jbd.host</code> alternately configure this setting.</li>
     * </ul><p>
     *
     * @param portRanges
     *          A comma separated list of ports and/or port ranges
     *          (ranges are inclusive and separated by a dash) -- e.g., <code>10000-10007,12500,12502,15376-15380</code>
     * @param host
     * @return this Builder
     */
    public Builder processes(String portRanges, String host) {
      this.portRanges = portRanges;
      this.processes = -1;
      this.host = host;
      return this;
    }

    /**
     * Whether to run in headless mode (no GUI windows).
     * <p>
     * Screenshots <i>are</i> available in headless mode (see {@link JBrowserDriver#getScreenshotAs(org.openqa.selenium.OutputType)}).
     * <p>
     * Defaults to <code>true</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.headless</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.headless</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param headless
     * @return this Builder
     */
    public Builder headless(boolean headless) {
      this.headless = headless;
      return this;
    }

    /**
     * Whether the hostname in certificates should be verified.
     * <p>
     * Defaults to <code>true</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.hostnameverification</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.hostnameverification</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param hostnameVerification
     * @return this Builder
     */
    public Builder hostnameVerification(boolean hostnameVerification) {
      this.hostnameVerification = hostnameVerification;
      return this;
    }

    /**
     * The idle time (no pending AJAX requests) required in milliseconds before a page is considered to have been loaded completely.
     * <p>
     * For very slow or overloaded CPUs, set a higher value.
     * <p>
     * Defaults to <code>150</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.ajaxwait</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.ajaxwait</code> alternately configures this setting.</li>
     * </ul><p>
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
     * whether all AJAX requests have completed.
     * <p>
     * Defaults to <code>2000</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.ajaxresourcetimeout</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.ajaxresourcetimeout</code> alternately configures this setting.</li>
     * </ul><p>
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
     * <p>
     * Based on hosts in ad-hosts.txt in the source tree.
     * <p>
     * Defaults to <code>false</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.blockads</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.blockads</code> alternately configures this setting.</li>
     * </ul><p>
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
     * <p>
     * These resources are still requested and can optionally be saved to disk (see the Settings options).
     * Some older versions of Java are inefficient (memory-wise) in rendering images.
     * <p>
     * Defaults to <code>false</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.quickrender</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.quickrender</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param quickRender
     * @return this Builder
     */
    public Builder quickRender(boolean quickRender) {
      this.quickRender = quickRender;
      return this;
    }

    /**
     * Maximum number of concurrent connections (per process) to a specific host+proxy combo.
     * <p>
     * Defaults to <code>8</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.maxrouteconnections</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.maxrouteconnections</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param maxRouteConnections
     * @return this Builder
     */
    public Builder maxRouteConnections(int maxRouteConnections) {
      this.maxRouteConnections = maxRouteConnections;
      return this;
    }

    /**
     * Maximum number of concurrent connections overall (per process).
     * <p>
     * Defaults to <code>300</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.maxconnections</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.maxconnections</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param maxConnections
     * @return this Builder
     */
    public Builder maxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
      return this;
    }

    /**
     * Whether javascript is enabled in the browser.
     * <p>
     * Defaults to <code>true</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.javascript</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.javascript</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param isEnabled
     *          <code>true</code> to enable Javascript, <code>false</code> otherwise
     * @return this Builder
     */
    public Builder javascript(boolean isEnabled) {
      this.javascript = isEnabled;
      return this;
    }

    /**
     * Socket timeout in milliseconds, which is the max idle time between any two packets.
     * <p>
     * Value of 0 means infinite timeout.
     * <p>
     * Defaults to -1 (system default).
     *
     * <p><ul>
     * <li>Java system property <code>jbd.sockettimeout</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.sockettimeout</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param timeoutMS
     *          Timeout in milliseconds
     * @return this Builder
     */
    public Builder socketTimeout(int timeoutMS) {
      this.socketTimeout = timeoutMS;
      return this;
    }

    /**
     * Connect timeout in milliseconds, which the is max time until a connection is established.
     * <p>
     * Value of 0 means infinite timeout.
     * <p>
     * Defaults to -1 (system default).
     *
     * <p><ul>
     * <li>Java system property <code>jbd.connecttimeout</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.connecttimeout</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param timeoutMS
     *          Timeout in milliseconds
     * @return this Builder
     */
    public Builder connectTimeout(int timeoutMS) {
      this.connectTimeout = timeoutMS;
      return this;
    }

    /**
     * Connection request timeout in milliseconds,
     * which is the max time to wait when the max number of connections has already been reached.
     * <p>
     * When the timeout is reached, the connection fails with an exception.
     * Value of 0 means infinite timeout.
     * <p>
     * Defaults to -1 (system default).
     *
     * <p><ul>
     * <li>Java system property <code>jbd.connectionreqtimeout</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.connectionreqtimeout</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param timeoutMS
     *          Timeout in milliseconds
     * @return this Builder
     */
    public Builder connectionReqTimeout(int timeoutMS) {
      this.connectionReqTimeout = timeoutMS;
      return this;
    }

    /**
     * Specifies a source of trusted certificate authorities.
     * <p>
     * Can take one of four values:
     * <br>(1) <code>compatible</code> to accept standard browser certs,
     * <br>(2) <code>trustanything</code> to accept any SSL cert,
     * <br>(3) a file path, or
     * <br>(4) a URL.
     * <br>If a file or URL is specified it must follow exactly the format of content like this: <a
     * href="https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt">https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt</a>
     * <p>The default when this property is not set (i.e., when it's <code>null</code>) is your JRE's keystore,
     * so you can use JDK's keytool to import specific certs.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.ssl</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.ssl</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param ssl
     * @return this Builder
     */
    public Builder ssl(String ssl) {
      this.ssl = ssl;
      return this;
    }

    /**
     * Log full requests and responses (excluding response bodies).
     * <p>
     * Defaults to <code>false</code>
     *
     * <p><ul>
     * <li>Java system property <code>jbd.logwire</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.logwire</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param logWire
     * @return this Builder
     */
    public Builder logWire(boolean logWire) {
      this.logWire = logWire;
      return this;
    }

    /**
     * Log the browser console output.
     * <p>
     * Defaults to <code>false</code>
     *
     * <p><ul>
     * <li>Java system property <code>jbd.logjavascript</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.logjavascript</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param logJavascript
     * @return this Builder
     */
    public Builder logJavascript(boolean logJavascript) {
      this.logJavascript = logJavascript;
      return this;
    }

    /**
     * Log details of HTTP requests performed and other info useful for monitoring runtime performance.
     * <p>
     * Defaults to <code>false</code>
     *
     * <p><ul>
     * <li>Java system property <code>jbd.logtrace</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.logtrace</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param logTrace
     * @return this Builder
     */
    public Builder logTrace(boolean logTrace) {
      this.logTrace = logTrace;
      return this;
    }

    /**
     * Log errors, exceptions, and important notices.
     * <p>
     * Defaults to <code>true</code>
     *
     * <p><ul>
     * <li>Java system property <code>jbd.logwarnings</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.logwarnings</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param logWarnings
     * @return this Builder
     */
    public Builder logWarnings(boolean logWarnings) {
      this.logWarnings = logWarnings;
      return this;
    }

    /**
     * At what log level the logger should log
     * <p>
     * Defaults to <code>Level.ALL</code>
     * <p>
     * The loggerLevel can be <code>null</code> meaning no logging will be done.
     *
     * @param loggerLevel
     * @return this Builder
     */
    public Builder loggerLevel(Level loggerLevel) {
      if (loggerLevel == null) {
        this.logger.setLevel(Level.OFF);
      } else {
        this.logger.setLevel(loggerLevel);
      }
      return this;
    }

    /**
     * The name of a Java Logger to handle log messages.
     * <p>
     * Logs are also available via the Selenium logging APIs--this logger has no effect on that.
     * <p>
     * The name can be <code>null</code> or <code>""</code> meaning no Java Logger will be used.
     * <p>
     * Defaults to <code>com.machinepublishers.jbrowserdriver</code> which echos messages to standard out/error.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.logger</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.logger</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param name
     * @return this Builder
     */
    public Builder logger(String name) {
      this.logger = name == null ? null : Logger.getLogger(name);
      return this;
    }

    /**
     * Maximum number of log messages (per log type) to store in memory (per process),
     * accessible via the Selenium logging APIs.
     * <p>
     * Logs are also available via a Java Logger--this max has no effect on that.
     * <p>
     * The oldest log entry is dropped once the max is reached. Regardless of this setting,
     * logs are cleared per instance of JBrowserDriver after a call to quit(), reset(), or Logs.get(String).
     * <p>
     * A value of zero disables Selenium logging.
     * <p>
     * Defaults to <code>1000</code>.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.logsmax</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.logsmax</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param logsMax
     * @return this Builder
     */
    public Builder logsMax(int logsMax) {
      this.logsMax = logsMax;
      return this;
    }

    /**
     * JVM options, such as Java system properties or Java HotSpot VM options.
     * <p>
     * Each option is a separate string in the array passed in. E.g.,
     * <p>
     * <code>
     * .javaOptions("-XX:+PrintCommandLineFlags", "-Xmx1g", "-Djsse.enableSNIExtension=false")
     * </code>
     * <p>
     * Note that browser instances are run in a separate Java process which doesn't inherit any
     * options set in the parent process (thus the need for this API).
     * <p>
     * By default no options are set.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.javaoptions</code> overrides this setting. The options must be separated by tab (\t).</li>
     * <li>{@link Capabilities} name <code>jbd.javaoptions</code> alternately configures this setting. The options must be separated by tab (\t).</li>
     * </ul><p>
     *
     * @param options
     * @return this Builder
     */
    public Builder javaOptions(String... options) {
      this.javaOptions = options;
      return this;
    }

    /**
     * The path to the Java executable or the Java command used to launch child JRE browser processes.
     * <p>
     * Defaults to <code>null</code>, meaning that the same Java executable running the parent process will be used to launch the child processes.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.javabinary</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.javabinary</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param javaBinary
     * @return this Builder
     */
    public Builder javaBinary(String javaBinary) {
      this.javaBinary = javaBinary;
      return this;
    }

    /**
     * Allows this library to run on Java 9. Enabling this option precludes running on Java 8.
     * <p>
     * Defaults to <code>false</code> (i.e., Java 8 compatible).
     *
     * <p><ul>
     * <li>Java system property <code>jbd.javaexportmodules</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.javaexportmodules</code> alternately configures this setting.</li>
     * </ul><p>
     *
     * @param javaExportModules
     * @return this Builder
     */
    public Builder javaExportModules(boolean javaExportModules) {
      this.javaExportModules = javaExportModules;
      return this;
    }

    /**
     * User data/local storage directory used by the browser. Callers may provide their own directory to share user data
     * across instances or to prevent the user data from being automatically deleted after resets or quits.
     * <p>
     * Defaults to <code>null</code>, meaning that a temp directory will be automatically created and destroyed as needed for user data.
     *
     * <p><ul>
     * <li>Java system property <code>jbd.userdatadirectory</code> overrides this setting. Use a string file path as the value.</li>
     * <li>{@link Capabilities} name <code>jbd.userdatadirectory</code> alternately configures this setting. Use a string file path as the value.</li>
     * </ul><p>
     * 
     * @param userDataDirectory
     *          A directory to store user website data, e.g. localStorage.
     * @return this Builder
     *
     * @see javafx.scene.web.WebEngine#userDataDirectory
     */
    public Builder userDataDirectory(File userDataDirectory) {
      this.userDataDirectory = userDataDirectory;
      return this;
    }

    /**
     * Used for binding to a specific NIC
     * 
     * @param nicAddress
     * @return this Builder
     */
    public Builder localIp(InetAddress nicAddress) {
      this.nicAddress = nicAddress;
      return this;
    }

    /**
     * Enables CSRF token handling. Searches for XSRF-TOKEN in response headers and sends X-XSRF-TOKEN in request headers.
     * 
     * @return this Builder
     */
    public Builder csrf() {
      return csrf("X-XSRF-TOKEN", "XSRF-TOKEN");
    }

    /**
     * Enables CSRF token handling
     * 
     * @param requestToken
     *          The header to send in each request header
     * @param responseToken
     *          The token to search for in response headers
     * @return this Builder
     */
    public Builder csrf(String requestToken, String responseToken) {
      this.csrfRequestToken = requestToken;
      this.csrfResponseToken = responseToken;
      return this;
    }

    /**
     * @deprecated Will be removed in v2.0.0. Instead use Settings Builder's logWire, logsMax, or logger.
     */
    @Deprecated
    public Builder wireConsole(boolean wireConsole) {
      System.err.println(
          "jBrowserDriver: The wireConsole setting is deprecated and will be removed in v2.0.0. Instead use Settings Builder's logWire, logsMax, or logger.");
      this.logWire = wireConsole;
      return this;
    }

    /**
     * @deprecated Will be removed in v2.0.0. Instead use Settings Builder's logTrace, logsMax, or logger.
     */
    @Deprecated
    public Builder traceConsole(boolean traceConsole) {
      System.err.println(
          "jBrowserDriver: The traceConsole setting is deprecated and will be removed in v2.0.0. Instead use Settings Builder's logTrace, logsMax, or logger.");
      this.logTrace = traceConsole;
      return this;
    }

    /**
     * @deprecated Will be removed in v2.0.0. Instead use Settings Builder's logWarnings, logsMax, or logger.
     */
    @Deprecated
    public Builder warnConsole(boolean warnConsole) {
      System.err.println(
          "jBrowserDriver: The warnConsole setting is deprecated and will be removed in v2.0.0. Instead use Settings Builder's logWarnings, logsMax, or logger.");
      this.logWarnings = warnConsole;
      return this;
    }

    /**
     * @deprecated Will be removed in v2.0.0. Instead use Settings Builder's logsMax, logWire, logTrace, or logWarnings.
     */
    @Deprecated
    public Builder maxLogs(int maxLogs) {
      System.err.println(
          "jBrowserDriver: The maxLogs setting is deprecated and will be removed in v2.0.0. Instead use Settings Builder's logsMax, logWire, logTrace, or logWarnings.");
      this.logsMax = maxLogs;
      return this;
    }

    /**
     * @return A Settings object created from this builder.
     * @see JBrowserDriver#JBrowserDriver(Settings)
     */
    public Settings build() {
      return new Settings(this, System.getProperties());
    }

    /**
     * @return A Selenium Capabilities object for a RemoteWebDriver or Selenium Server
     */
    public Capabilities buildCapabilities() {
      DesiredCapabilities capabilities = new DesiredCapabilities("jbrowserdriver", "", Platform.ANY);
      set(capabilities, PropertyName.CACHE_ENTRY_SIZE, this.cacheEntrySize);
      set(capabilities, PropertyName.CACHE_ENTRIES, this.cacheEntries);
      set(capabilities, PropertyName.CACHE, this.cache);
      set(capabilities, PropertyName.IGNORE_DIALOGS, this.ignoreDialogs);
      set(capabilities, PropertyName.SAVE_ATTACHMENTS, this.saveAttachments);
      set(capabilities, PropertyName.SAVE_MEDIA, this.saveMedia);
      set(capabilities, PropertyName.AJAX_WAIT, this.ajaxWait);
      set(capabilities, PropertyName.AJAX_RESOURCE_TIMEOUT, this.ajaxResourceTimeout);
      set(capabilities, PropertyName.BLOCK_ADS, this.blockAds);
      set(capabilities, PropertyName.QUICK_RENDER, this.quickRender);
      set(capabilities, PropertyName.MAX_ROUTE_CONNECTIONS, this.maxRouteConnections);
      set(capabilities, PropertyName.MAX_CONNECTIONS, this.maxConnections);
      set(capabilities, PropertyName.LOG_WIRE, this.logWire);
      set(capabilities, PropertyName.LOG_JAVASCRIPT, this.logJavascript);
      set(capabilities, PropertyName.LOG_TRACE, this.logTrace);
      set(capabilities, PropertyName.LOG_WARNINGS, this.logWarnings);
      set(capabilities, PropertyName.LOGS_MAX, this.logsMax);
      set(capabilities, PropertyName.LOGGER, this.logger);
      set(capabilities, PropertyName.HEAD_SCRIPT, this.headScript);
      set(capabilities, PropertyName.HOST, this.host);
      set(capabilities, PropertyName.PORT_RANGES, this.portRanges == null || this.portRanges.isEmpty() ? null : this.portRanges);
      if (this.processes > -1) {
        set(capabilities, PropertyName.PROCESSES, this.processes);
      }
      set(capabilities, PropertyName.HEADLESS, this.headless);
      set(capabilities, PropertyName.SSL, this.ssl);
      set(capabilities, PropertyName.HOSTNAME_VERIFICATION, this.hostnameVerification);
      set(capabilities, PropertyName.JAVASCRIPT, this.javascript);
      set(capabilities, PropertyName.SOCKET_TIMEOUT_MS, this.socketTimeout);
      set(capabilities, PropertyName.CONNECT_TIMEOUT_MS, this.connectTimeout);
      set(capabilities, PropertyName.CONNECTION_REQ_TIMEOUT_MS, this.connectionReqTimeout);
      set(capabilities, PropertyName.JAVA_OPTIONS, StringUtils.join(this.javaOptions, "\t"));
      set(capabilities, PropertyName.JAVA_BINARY, this.javaBinary);
      set(capabilities, PropertyName.JAVA_EXPORT_MODULES, this.javaExportModules);

      if (this.screen != null) {
        set(capabilities, PropertyName.SCREEN_WIDTH, this.screen.getWidth());
        set(capabilities, PropertyName.SCREEN_HEIGHT, this.screen.getHeight());
      }

      if (this.cacheDir != null) {
        set(capabilities, PropertyName.CACHE_DIR, this.cacheDir.getAbsolutePath());
      }

      if (this.userDataDirectory != null) {
        capabilities.setCapability(PropertyName.USER_DATA_DIRECTORY.propertyName, this.userDataDirectory.getAbsolutePath());
      }

      if (this.timezone != null) {
        set(capabilities, PropertyName.TIMEZONE, this.timezone.name());
      }

      if (RequestHeaders.TOR.equals(this.requestHeaders)
          && UserAgent.TOR.equals(this.userAgent)) {
        set(capabilities, PropertyName.USER_AGENT, "tor");
      } else if (RequestHeaders.CHROME.equals(this.requestHeaders)
          && UserAgent.CHROME.equals(this.userAgent)) {
        set(capabilities, PropertyName.USER_AGENT, "chrome");
      }

      if (this.proxy != null && this.proxy.type() != null) {
        set(capabilities, PropertyName.PROXY_TYPE, proxy.type().toString());
        set(capabilities, PropertyName.PROXY_HOST, proxy.host());
        set(capabilities, PropertyName.PROXY_PORT, proxy.port());
        set(capabilities, PropertyName.PROXY_USERNAME, proxy.user());
        set(capabilities, PropertyName.PROXY_PASSWORD, proxy.password());
        set(capabilities, PropertyName.PROXY_EXPECT_CONTINUE, proxy.expectContinue());
      }

      return capabilities;
    }

    Settings build(Capabilities capabilities) {
      Map properties = new HashMap(capabilities.asMap());
      Proxy proxy = Proxy.extractFrom(capabilities);
      if (proxy != null) {
        String proxyConfigString = proxy.getHttpProxy();
        if (proxyConfigString != null) {
          Pattern pattern = Pattern.compile("(?:http(?:s)?:\\/\\/)?(?:([^:@]*):([^:@]*)@)?([^:@]*)(?::(\\d+))?");
          Matcher matcher = pattern.matcher(proxyConfigString);
          if (matcher.matches()) {
            properties.put(PropertyName.PROXY_TYPE.propertyName, ProxyConfig.Type.HTTP);
            properties.put(PropertyName.PROXY_USERNAME.propertyName, matcher.group(1));
            properties.put(PropertyName.PROXY_PASSWORD.propertyName, matcher.group(2));
            properties.put(PropertyName.PROXY_HOST.propertyName, matcher.group(3));
            properties.put(PropertyName.PROXY_PORT.propertyName, matcher.group(4));
          }
        }
      }
      for (Map.Entry entry : System.getProperties().entrySet()) {
        properties.put(entry.getKey(), entry.getValue());
      }
      return new Settings(this, properties);
    }
  }

  private static long nextAnonPort() {
    --curAnonPort;
    if (curAnonPort > -1) {
      curAnonPort = -1;
    }
    return curAnonPort;
  }

  private static List<PortGroup> parsePorts(int processesMax) {
    List<PortGroup> portGroups = new ArrayList<PortGroup>();
    synchronized (curAnonPortLock) {
      for (int i = 0; i < processesMax; i++) {
        portGroups.add(new PortGroup(nextAnonPort(), nextAnonPort(), nextAnonPort()));
      }
    }
    return Collections.unmodifiableList(portGroups);
  }

  private static List<PortGroup> parsePorts(String portString) {
    Collection<Long> ports = new LinkedHashSet<Long>();
    String[] ranges = portString.split(",");
    for (int i = 0; i < ranges.length; i++) {
      String[] bounds = ranges[i].split("-");
      long low = Long.parseLong(bounds[0]);
      long high = bounds.length > 1 ? Long.parseLong(bounds[1]) : low;
      for (long j = low; j <= high; j++) {
        ports.add(j);
      }
    }

    if (ports.size() % 3 != 0) {
      throw new IllegalArgumentException("Each process requires three ports (i.e., number of ports must be a multiple of three).");
    }

    int max = ports.size() / 3;
    Iterator<Long> iter = ports.iterator();
    List<PortGroup> portGroups = new ArrayList<PortGroup>();
    for (int i = 0; i < max; i++) {
      portGroups.add(new PortGroup(iter.next(), iter.next(), iter.next()));
    }
    return Collections.unmodifiableList(portGroups);
  }

  private static void set(DesiredCapabilities capabilities, PropertyName name, int val) {
    capabilities.setCapability(name.propertyName, Integer.toString(val));
  }

  private static void set(DesiredCapabilities capabilities, PropertyName name, long val) {
    capabilities.setCapability(name.propertyName, Long.toString(val));
  }

  private static void set(DesiredCapabilities capabilities, PropertyName name, boolean val) {
    capabilities.setCapability(name.propertyName, Boolean.toString(val));
  }

  private static void set(DesiredCapabilities capabilities, PropertyName name, Logger val) {
    capabilities.setCapability(name.propertyName, val == null ? null : val.getName());
  }

  private static void set(DesiredCapabilities capabilities, PropertyName name, String val) {
    if (val != null) {
      capabilities.setCapability(name.propertyName, val);
    }
  }

  private static int parse(Map capabilities, PropertyName name, int fallback) {
    if (capabilities.get(name.propertyName) != null) {
      return Integer.parseInt(capabilities.get(name.propertyName).toString());
    }
    return fallback;
  }

  private static long parse(Map capabilities, PropertyName name, long fallback) {
    if (capabilities.get(name.propertyName) != null) {
      return Long.parseLong(capabilities.get(name.propertyName).toString());
    }
    return fallback;
  }

  private static boolean parse(Map capabilities, PropertyName name, boolean fallback) {
    if (capabilities.get(name.propertyName) != null) {
      return Boolean.parseBoolean(capabilities.get(name.propertyName).toString());
    }
    return fallback;
  }

  private static String parse(Map capabilities, PropertyName name, String fallback) {
    if (capabilities.get(name.propertyName) != null) {
      return capabilities.get(name.propertyName).toString();
    }
    return fallback;
  }

  private static Logger parse(Map capabilities, PropertyName name, Logger fallback) {
    if (capabilities.containsKey(name.propertyName)) {
      if (capabilities.get(name.propertyName) == null || capabilities.get(name.propertyName).equals("")) {
        return null;
      }
      return Logger.getLogger(capabilities.get(name.propertyName).toString());
    }
    return fallback;
  }

  private static File parse(Map capabilities, PropertyName name, File fallback) {
    if (capabilities.containsKey(name.propertyName)) {
      Object pathName = capabilities.get(name.propertyName);
      if (pathName == null || pathName.equals("")) {
        return null;
      }
      return new File(pathName.toString());
    }
    return fallback;
  }

  private static long curAnonPort;
  private static final Object curAnonPortLock = new Object();
  private final RequestHeaders requestHeaders;
  private final int screenWidth;
  private final int screenHeight;
  private final String userAgentString;
  private final ProxyConfig proxy;
  private final boolean saveMedia;
  private final boolean saveAttachments;
  private final String script;
  private final String scriptContent;
  private final boolean ignoreDialogs;
  private final boolean cache;
  private final File cacheDir;
  private final int cacheEntries;
  private final long cacheEntrySize;
  private final List<PortGroup> portGroups;
  private final boolean headless;
  private final long ajaxWait;
  private final long ajaxResourceTimeout;
  private final boolean blockAds;
  private final boolean quickRender;
  private final int maxRouteConnections;
  private final int maxConnections;
  private final String ssl;
  private final boolean logWire;
  private final boolean logJavascript;
  private final boolean logTrace;
  private final boolean logWarnings;
  private final int logsMax;
  private final transient Logger logger;
  private final int loggerLevel;
  private final boolean hostnameVerification;
  private final boolean javascript;
  private final int socketTimeout;
  private final int connectTimeout;
  private final int connectionReqTimeout;
  private final String host;
  private final List<String> javaOptions;
  private final boolean customClasspath;
  private final String javaBinary;
  private final boolean javaExportModules;
  private final File userDataDirectory;
  private final String csrfRequestToken;
  private final String csrfResponseToken;
  private final InetAddress nicAddress;

  private Settings(Settings.Builder builder, Map properties) {
    Settings.Builder defaults = Settings.builder();

    Dimension screen = builder.screen == null ? defaults.screen : builder.screen;
    this.screenWidth = parse(properties, PropertyName.SCREEN_WIDTH, screen.getWidth());
    this.screenHeight = parse(properties, PropertyName.SCREEN_HEIGHT, screen.getHeight());
    this.cacheEntrySize = parse(properties, PropertyName.CACHE_ENTRY_SIZE, builder.cacheEntrySize);
    this.cacheEntries = parse(properties, PropertyName.CACHE_ENTRIES, builder.cacheEntries);
    this.cache = parse(properties, PropertyName.CACHE, builder.cache);
    this.ignoreDialogs = parse(properties, PropertyName.IGNORE_DIALOGS, builder.ignoreDialogs);
    this.saveAttachments = parse(properties, PropertyName.SAVE_ATTACHMENTS, builder.saveAttachments);
    this.saveMedia = parse(properties, PropertyName.SAVE_MEDIA, builder.saveMedia);
    this.ajaxWait = parse(properties, PropertyName.AJAX_WAIT, builder.ajaxWait);
    this.ajaxResourceTimeout = parse(properties, PropertyName.AJAX_RESOURCE_TIMEOUT, builder.ajaxResourceTimeout);
    this.blockAds = parse(properties, PropertyName.BLOCK_ADS, builder.blockAds);
    this.quickRender = parse(properties, PropertyName.QUICK_RENDER, builder.quickRender);
    this.maxRouteConnections = parse(properties, PropertyName.MAX_ROUTE_CONNECTIONS, builder.maxRouteConnections);
    this.maxConnections = parse(properties, PropertyName.MAX_CONNECTIONS, builder.maxConnections);
    this.logJavascript = parse(properties, PropertyName.LOG_JAVASCRIPT, builder.logJavascript);
    if (properties.get(PropertyName.JAVA_OPTIONS.propertyName) != null) {
      this.javaOptions = Collections.unmodifiableList(Arrays.asList(
          properties.get(PropertyName.JAVA_OPTIONS.propertyName).toString().split("\t")));
    } else if (builder.javaOptions == null) {
      this.javaOptions = Collections.unmodifiableList(new ArrayList<String>());
    } else {
      this.javaOptions = Collections.unmodifiableList(Arrays.asList(builder.javaOptions));
    }
    this.customClasspath = this.javaOptions.contains("-classpath") || this.javaOptions.contains("-cp");
    this.javaBinary = parse(properties, PropertyName.JAVA_BINARY, builder.javaBinary);
    this.javaExportModules = parse(properties, PropertyName.JAVA_EXPORT_MODULES, builder.javaExportModules);
    if (properties.get(PropertyName.WIRE_CONSOLE.propertyName) != null) {
      System.err.println("jBrowserDriver: The jbd.wireconsole setting is deprecated and will be removed in v2.0.0. Use jbd.logwire, jbd.logger, or jbd.logsmax instead.");
      this.logWire = parse(properties, PropertyName.WIRE_CONSOLE, builder.logWire);
    } else {
      this.logWire = parse(properties, PropertyName.LOG_WIRE, builder.logWire);
    }
    if (properties.get(PropertyName.TRACE_CONSOLE.propertyName) != null) {
      System.err.println("jBrowserDriver: The jbd.traceconsole setting is deprecated and will be removed in v2.0.0. Use jbd.logtrace, jbd.logger, or jbd.logsmax instead.");
      this.logTrace = parse(properties, PropertyName.TRACE_CONSOLE, builder.logTrace);
    } else {
      this.logTrace = parse(properties, PropertyName.LOG_TRACE, builder.logTrace);
    }
    if (properties.get(PropertyName.WARN_CONSOLE.propertyName) != null) {
      System.err.println("jBrowserDriver: The jbd.warnconsole setting is deprecated and will be removed in v2.0.0. Use jbd.logwarnings, jbd.logger, or jbd.logsmax instead.");
      this.logWarnings = parse(properties, PropertyName.WARN_CONSOLE, builder.logWarnings);
    } else {
      this.logWarnings = parse(properties, PropertyName.LOG_WARNINGS, builder.logWarnings);
    }
    if (properties.get(PropertyName.MAX_LOGS.propertyName) != null) {
      System.err.println("jBrowserDriver: The jbd.maxlogs setting is deprecated and will be removed in v2.0.0. Use jbd.logsmax, jbd.logwire, jbd.logtrace, or jbd.logwarnings instead.");
      this.logsMax = parse(properties, PropertyName.MAX_LOGS, builder.logsMax);
    } else {
      this.logsMax = parse(properties, PropertyName.LOGS_MAX, builder.logsMax);
    }
    this.logger = parse(properties, PropertyName.LOGGER, builder.logger);
    if (this.logger == null) {
      this.loggerLevel = Level.OFF.intValue();
    } else {
      Level curLevel = logger.getLevel();
      this.loggerLevel = curLevel == null ? Level.INFO.intValue() : curLevel.intValue();
    }
    this.hostnameVerification = parse(properties, PropertyName.HOSTNAME_VERIFICATION, builder.hostnameVerification);
    this.javascript = parse(properties, PropertyName.JAVASCRIPT, builder.javascript);
    this.socketTimeout = parse(properties, PropertyName.SOCKET_TIMEOUT_MS, builder.socketTimeout);
    this.connectTimeout = parse(properties, PropertyName.CONNECT_TIMEOUT_MS, builder.connectTimeout);
    this.connectionReqTimeout = parse(properties, PropertyName.CONNECTION_REQ_TIMEOUT_MS, builder.connectionReqTimeout);
    this.cacheDir = parse(properties, PropertyName.CACHE_DIR, builder.cacheDir);
    this.userDataDirectory = parse(properties, PropertyName.USER_DATA_DIRECTORY, builder.userDataDirectory);
    this.csrfRequestToken = parse(properties, PropertyName.CSRF_REQUEST_TOKEN, builder.csrfRequestToken);
    this.csrfResponseToken = parse(properties, PropertyName.CSRF_RESPONSE_TOKEN, builder.csrfResponseToken);
    this.host = parse(properties, PropertyName.HOST, builder.host);
    String portRangesTmp = null;
    int processesTmp = -1;
    if (properties.get(PropertyName.PORTS.propertyName) == null
        && properties.get(PropertyName.PORT_RANGES.propertyName) == null
        && properties.get(PropertyName.PROCESSES.propertyName) == null) {
      if (builder.processes > -1) {
        processesTmp = builder.processes;
      } else {
        portRangesTmp = builder.portRanges;
      }
    } else if (properties.get(PropertyName.PORTS.propertyName) != null) {
      System.err.println("jBrowserDriver: The jbd.ports property is deprecated and will be removed in v2.0.0. Refer to Settings.Builder.processes(..) API documentation.");
      String portString = properties.get(PropertyName.PORTS.propertyName).toString();
      Collection<Long> ports = new LinkedHashSet<Long>();
      String[] ranges = portString.split(",");
      for (String range : ranges) {
        String[] bounds = range.split("-");
        long low = Long.parseLong(bounds[0]);
        long high = bounds.length > 1 ? Long.parseLong(bounds[1]) : low;
        for (long j = low; j <= high; j++) {
          ports.add(j);
        }
      }
      processesTmp = ports.size();
    } else if (properties.get(PropertyName.PORT_RANGES.propertyName) != null) {
      portRangesTmp = properties.get(PropertyName.PORT_RANGES.propertyName).toString();
    } else {
      processesTmp = Integer.parseInt(properties.get(PropertyName.PROCESSES.propertyName).toString());
    }
    if (portRangesTmp == null) {
      this.portGroups = parsePorts(processesTmp);
    } else {
      this.portGroups = parsePorts(portRangesTmp);
    }

    //backwards compatible property name for versions <= 0.9.1
    boolean headlessTmp = parse(properties, PropertyName.HEADLESS, builder.headless);
    headlessTmp = System.getProperty(PropertyName.HEADLESS.propertyName) == null
        && System.getProperty("jbd.browsergui") != null
            ? !Boolean.parseBoolean(System.getProperty("jbd.browsergui")) : headlessTmp;
    this.headless = headlessTmp;
    if (System.getProperty("jbd.browsergui") != null) {
      System.err.println("jBrowserDriver: The jbd.browsergui property is deprecated and will be removed in v2.0.0. Use jbd.headless property instead.");
    }

    //backwards compatible property name for versions <= 0.9.1
    String sslTmp = parse(properties, PropertyName.SSL, builder.ssl);
    sslTmp = System.getProperty(PropertyName.SSL.propertyName) == null
        && System.getProperty("jbd.pemfile") != null
            ? System.getProperty("jbd.pemfile") : sslTmp;
    this.ssl = sslTmp;
    if (System.getProperty("jbd.pemfile") != null) {
      System.err.println("jBrowserDriver: The jbd.pemfile property is deprecated and will be removed in v2.0.0. Use jbd.ssl property instead.");
    }

    RequestHeaders requestHeadersTmp = builder.requestHeaders;
    UserAgent userAgentTmp = builder.userAgent;
    if (properties.get(PropertyName.USER_AGENT.propertyName) != null) {
      String value = properties.get(PropertyName.USER_AGENT.propertyName).toString();
      if ("tor".equalsIgnoreCase(value)) {
        requestHeadersTmp = RequestHeaders.TOR;
        userAgentTmp = UserAgent.TOR;
      } else if ("chrome".equalsIgnoreCase(value)) {
        requestHeadersTmp = RequestHeaders.CHROME;
        userAgentTmp = UserAgent.CHROME;
      }
    }
    requestHeadersTmp = requestHeadersTmp == null ? defaults.requestHeaders : requestHeadersTmp;
    userAgentTmp = userAgentTmp == null ? defaults.userAgent : userAgentTmp;
    this.requestHeaders = requestHeadersTmp;
    this.userAgentString = userAgentTmp.userAgentString();
    this.nicAddress = builder.nicAddress;

    ProxyConfig proxyTmp = builder.proxy;
    if (properties.get(PropertyName.PROXY_TYPE.propertyName) != null
        && properties.get(PropertyName.PROXY_HOST.propertyName) != null
        && properties.get(PropertyName.PROXY_PORT.propertyName) != null) {
      ProxyConfig.Type type = ProxyConfig.Type.valueOf(properties.get(PropertyName.PROXY_TYPE.propertyName).toString());
      String host = properties.get(PropertyName.PROXY_HOST.propertyName).toString();
      int port = Integer.parseInt(properties.get(PropertyName.PROXY_PORT.propertyName).toString());
      String username = parse(properties, PropertyName.PROXY_USERNAME, (String) null);
      String password = parse(properties, PropertyName.PROXY_PASSWORD, (String) null);
      Object expectContinue = properties.get(PropertyName.PROXY_EXPECT_CONTINUE.propertyName);
      if (expectContinue == null) {
        proxyTmp = new ProxyConfig(type, host, port, username, password);
      } else {
        proxyTmp = new ProxyConfig(type, host, port, username, password,
            Boolean.parseBoolean(expectContinue.toString()));
      }
    }
    this.proxy = proxyTmp;

    Timezone timezoneTmp = builder.timezone;
    if (properties.get(PropertyName.TIMEZONE.propertyName) != null) {
      timezoneTmp = Timezone.byName(properties.get(PropertyName.TIMEZONE.propertyName).toString());
    }
    timezoneTmp = timezoneTmp == null ? defaults.timezone : timezoneTmp;
    String scriptId = Util.randomPropertyName();
    String headScriptTmp = parse(properties, PropertyName.HEAD_SCRIPT, builder.headScript);
    StringBuilder scriptBuilder = new StringBuilder();
    StringBuilder scriptContentBuilder = new StringBuilder();
    scriptBuilder.append("<script id='").append(scriptId).append("' language='javascript'>");
    scriptContentBuilder.append("(function(){try{");
    scriptContentBuilder.append(userAgentTmp.script());
    scriptContentBuilder.append(timezoneTmp.script());
    if (headScriptTmp != null) {
      scriptContentBuilder.append(headScriptTmp);
    }
    scriptContentBuilder.append("}catch(e){}})();");
    this.scriptContent = scriptContentBuilder.toString();
    scriptBuilder.append(this.scriptContent);
    scriptBuilder.append("(function(){document.getElementsByTagName('head')[0].removeChild(document.getElementById('");
    scriptBuilder.append(scriptId);
    scriptBuilder.append("'));})();");
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

  String scriptContent() {
    return scriptContent;
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

  List<PortGroup> portGroups() {
    return portGroups;
  }

  String host() {
    return host;
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

  boolean javascript() {
    return javascript;
  }

  int socketTimeout() {
    return socketTimeout;
  }

  int connectTimeout() {
    return connectTimeout;
  }

  int connectionReqTimeout() {
    return connectionReqTimeout;
  }

  boolean hostnameVerification() {
    return hostnameVerification;
  }

  boolean logWire() {
    return logWire;
  }

  boolean logJavascript() {
    return logJavascript;
  }

  boolean logTrace() {
    return logTrace;
  }

  boolean logWarnings() {
    return logWarnings;
  }

  int logsMax() {
    return logsMax;
  }

  Logger logger() {
    return logger;
  }

  int loggerLevel() {
    return loggerLevel;
  }

  List<String> javaOptions() {
    return javaOptions;
  }

  String javaBinary() {
    return javaBinary;
  }

  boolean customClasspath() {
    return customClasspath;
  }

  boolean javaExportModules() {
    return javaExportModules;
  }

  File userDataDirectory() {
    return userDataDirectory;
  }

  String getCsrfRequestToken() {
    return csrfRequestToken;
  }

  String getCsrfResponseToken() {
    return csrfResponseToken;
  }

  InetAddress getLocalIp() {
    return nicAddress;
  }
}
