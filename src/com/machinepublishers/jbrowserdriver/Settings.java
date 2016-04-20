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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.DesiredCapabilities;

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

  private static enum PropertyName {
    @Deprecated PORTS("jbd.ports"),
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
    WIRE_LOG("jbd.wirelog"),
    JAVASCRIPT_LOG("jbd.javascriptlog"),
    TRACE_LOG("jbd.tracelog"),
    WARN_LOG("jbd.warnlog"),
    WIRE_CONSOLE("jbd.wireconsole"),
    JAVASCRIPT_CONSOLE("jbd.javascriptconsole"),
    TRACE_CONSOLE("jbd.traceconsole"),
    WARN_CONSOLE("jbd.warnconsole"),
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
    private Collection<Integer> ports = new LinkedHashSet<Integer>();
    private int processes = 2 * Runtime.getRuntime().availableProcessors();
    private boolean headless = true;
    private long ajaxWait = 150;
    private long ajaxResourceTimeout = 2000;
    private boolean blockAds = true;
    private boolean quickRender = true;
    private int maxRouteConnections = 8;
    private int maxConnections = 300;
    private String ssl;
    private boolean wireLog;
    private boolean javascriptLog;
    private boolean traceLog;
    private boolean warnLog = true;
    private boolean wireConsole;
    private boolean javascriptConsole;
    private boolean traceConsole;
    private boolean warnConsole = true;
    private int maxLogs = 1000;
    private boolean hostnameVerification = true;
    private boolean javascript = true;
    private int socketTimeout = -1;
    private int connectTimeout = -1;
    private int connectionReqTimeout = -1;
    private String host = "127.0.0.1";
    //TODO    private ResponseInterceptor[] responseInterceptors;

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
     * file is the original URL of the request for this media, and the second line is
     * the mime type).
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
     * @deprecated Will be removed. Use Settings.Builder.processes(..) instead.
     */
    @Deprecated
    public Builder ports(int... ports) {
      System.err.println("jBrowserDriver: The ports setting is deprecated and will be removed. Use Settings.Builder.processes(..) instead.");
      this.ports.clear();
      this.processes = -1;
      this.host = "127.0.0.1";
      this.ports.add(0);
      for (int i = 0; ports != null && i < ports.length; i++) {
        this.ports.add(ports[i]);
      }
      return this;
    }

    /**
     * @deprecated Will be removed. Use Settings.Builder.processes(..) instead.
     */
    @Deprecated
    public Builder portsMax(int startingPort, int maxProcesses) {
      System.err.println("jBrowserDriver: The portsMax setting is deprecated and will be removed. Use Settings.Builder.processes(..) instead.");
      this.ports.clear();
      this.processes = -1;
      this.host = "127.0.0.1";
      this.ports.add(0);
      for (int i = 0; i < maxProcesses; i++) {
        this.ports.add(startingPort + i);
      }
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
      this.ports.clear();
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
      this.ports.clear();
      this.processes = maxProcesses;
      this.host = host;
      return this;
    }

    /**
     * The ports used by {@link JBrowserDriver} instances and the parent process.
     * 
     * The max number of instances that can run concurrently is inferred from the number of ports provided
     * (which will be one less than the number of ports provided, to account for the port dedicated to the parent process).
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
     *          (which are inclusive and separated by a dash) -- e.g., <code>10000-10007,12500,12502,15377-15380</code>
     * @return this Builder
     */
    public Builder processes(String portRanges) {
      this.ports.clear();
      this.ports.addAll(parsePorts(portRanges));
      this.processes = -1;
      this.host = "127.0.0.1";
      return this;
    }

    /**
     * The ports and host/IP used by {@link JBrowserDriver} instances and the parent process.
     * 
     * The max number of instances that can run concurrently is inferred from the number of ports provided
     * (which will be one less than the number of ports provided, to account for the port dedicated to the parent process).
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
     *          (which are inclusive and separated by a dash) -- e.g., <code>10000-10007,12500,12502,15377-15380</code>
     * @param host
     * @return this Builder
     */
    public Builder processes(String portRanges, String host) {
      this.ports.clear();
      this.ports.addAll(parsePorts(portRanges));
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
     * Defaults to <code>true</code>.
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
     * Some versions of Java are inefficient (memory-wise) in rendering images.
     * <p>
     * Defaults to <code>true</code>.
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

    //TODO
    //    /**
    //     * <p><ul>
    //     * <li>Java system property <code>jbd.responseinterceptors</code> overrides this setting.</li>
    //     * <li>{@link Capabilities} name <code>jbd.responseinterceptors</code> alternately configures this setting.</li>
    //     * <li>Note that the value must be a base-64 encoded string of a serialized ResponseInterceptor array.
    //     * </ul><p>
    //     * 
    //     * @param responseInterceptors
    //     *          Interceptors to modify the response before it's passed to the browser.
    //     * 
    //     * @return this Builder
    //     */
    //    public Builder responseInterceptors(ResponseInterceptor... responseInterceptors) {
    //      this.responseInterceptors = Arrays.copyOf(responseInterceptors, responseInterceptors.length);
    //      return this;
    //    }

    /**
     * Send full requests and responses (except response bodies) to standard out.
     * <p>
     * Defaults to <code>false</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.wireconsole</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.wireconsole</code> alternately configures this setting.</li>
     * </ul><p>
     * 
     * @param wireConsole
     * @return this Builder
     */
    public Builder wireConsole(boolean wireConsole) {
      this.wireConsole = wireConsole;
      return this;
    }

    /**
     * Send Javascript browser messages to standard out.
     * <p>
     * Defaults to <code>false</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.javascriptconsole</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.javascriptconsole</code> alternately configures this setting.</li>
     * </ul><p>
     * 
     * @param javascriptConsole
     * @return this Builder
     */
    public Builder javascriptConsole(boolean javascriptConsole) {
      this.javascriptConsole = javascriptConsole;
      return this;
    }

    /**
     * Send trace messages to standard out.
     * <p>
     * Defaults to <code>false</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.traceconsole</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.traceconsole</code> alternately configures this setting.</li>
     * </ul><p>
     * 
     * @param traceConsole
     * @return this Builder
     */
    public Builder traceConsole(boolean traceConsole) {
      this.traceConsole = traceConsole;
      return this;
    }

    /**
     * Send important messages to standard error.
     * <p>
     * Defaults to <code>true</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.warnconsole</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.warnconsole</code> alternately configures this setting.</li>
     * </ul><p>
     * 
     * @param warnConsole
     * @return this Builder
     */
    public Builder warnConsole(boolean warnConsole) {
      this.warnConsole = warnConsole;
      return this;
    }

    /**
     * Send full requests and responses (except response bodies) to Selenium logger.
     * <p>
     * Defaults to <code>false</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.wirelog</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.wirelog</code> alternately configures this setting.</li>
     * </ul><p>
     * 
     * @param wireLog
     * @return this Builder
     * @see Settings.Builder#maxLogs(int)
     */
    public Builder wireLog(boolean wireLog) {
      this.wireLog = wireLog;
      return this;
    }

    /**
     * Send Javascript browser messages to Selenium logger.
     * <p>
     * Defaults to <code>false</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.javascriptlog</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.javascriptlog</code> alternately configures this setting.</li>
     * </ul><p>
     * 
     * @param javascriptLog
     * @return this Builder
     * @see Settings.Builder#maxLogs(int)
     */
    public Builder javascriptLog(boolean javascriptLog) {
      this.javascriptLog = javascriptLog;
      return this;
    }

    /**
     * Send trace messages to Selenium logger.
     * <p>
     * Defaults to <code>false</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.tracelog</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.tracelog</code> alternately configures this setting.</li>
     * </ul><p>
     * 
     * @param traceLog
     * @return this Builder
     * @see Settings.Builder#maxLogs(int)
     */
    public Builder traceLog(boolean traceLog) {
      this.traceLog = traceLog;
      return this;
    }

    /**
     * Send important messages to Selenium logger.
     * <p>
     * Defaults to <code>true</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.warnlog</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.warnlog</code> alternately configures this setting.</li>
     * </ul><p>
     * 
     * @param warnLog
     * @return this Builder
     * @see Settings.Builder#maxLogs(int)
     */
    public Builder warnLog(boolean warnLog) {
      this.warnLog = warnLog;
      return this;
    }

    /**
     * Maximum number of log entries (per type) to store in memory (per process), accessible via the Selenium logger.
     * <p>
     * The oldest log entry is dropped once the max is reached. Regardless of this setting,
     * logs are cleared per instance of JBrowserDriver after a call to quit(), reset(), or Logs.get(String).
     * <p>
     * Defaults to <code>1000</code>.
     * 
     * <p><ul>
     * <li>Java system property <code>jbd.maxlogs</code> overrides this setting.</li>
     * <li>{@link Capabilities} name <code>jbd.maxlogs</code> alternately configures this setting.</li>
     * </ul><p>
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
      set(capabilities, PropertyName.WIRE_CONSOLE, this.wireConsole);
      set(capabilities, PropertyName.JAVASCRIPT_CONSOLE, this.javascriptConsole);
      set(capabilities, PropertyName.TRACE_CONSOLE, this.traceConsole);
      set(capabilities, PropertyName.WARN_CONSOLE, this.warnConsole);
      set(capabilities, PropertyName.WIRE_LOG, this.wireLog);
      set(capabilities, PropertyName.JAVASCRIPT_LOG, this.javascriptLog);
      set(capabilities, PropertyName.TRACE_LOG, this.traceLog);
      set(capabilities, PropertyName.WARN_LOG, this.warnLog);
      set(capabilities, PropertyName.MAX_LOGS, this.maxLogs);
      set(capabilities, PropertyName.HEAD_SCRIPT, this.headScript);
      set(capabilities, PropertyName.HOST, this.host);
      final String joinedPorts = StringUtils.join(this.ports, ',');
      set(capabilities, PropertyName.PORT_RANGES, joinedPorts == null || joinedPorts.isEmpty() ? null : joinedPorts);
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
      //TODO set(capabilities, PropertyName.RESPONSE_INTERCEPTORS, this.responseInterceptors);

      if (this.screen != null) {
        set(capabilities, PropertyName.SCREEN_WIDTH, this.screen.getWidth());
        set(capabilities, PropertyName.SCREEN_HEIGHT, this.screen.getHeight());
      }

      if (this.cacheDir != null) {
        set(capabilities, PropertyName.CACHE_DIR, this.cacheDir.getAbsolutePath());
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
      for (Map.Entry entry : System.getProperties().entrySet()) {
        properties.put(entry.getKey(), entry.getValue());
      }
      return new Settings(this, properties);
    }
  }

  private static List<Integer> parsePorts(String portString) {
    Collection<Integer> ports = new LinkedHashSet<Integer>();
    String[] ranges = portString.split(",");
    for (int i = 0; i < ranges.length; i++) {
      String[] bounds = ranges[i].split("-");
      int low = Integer.parseInt(bounds[0]);
      int high = bounds.length > 1 ? Integer.parseInt(bounds[1]) : low;
      for (int j = low; j <= high; j++) {
        ports.add(j);
      }
    }
    return new ArrayList<Integer>(ports);
  }

  private static List<Integer> parseProcesses(String processesMax) {
    int max = Integer.parseInt(processesMax);
    List<Integer> ports = new ArrayList<Integer>();
    int curPort = -1;
    for (int i = 0; i < max + 1; i++) {
      ports.add(curPort--);
    }
    return ports;
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

  private static void set(DesiredCapabilities capabilities, PropertyName name, String val) {
    if (val != null) {
      capabilities.setCapability(name.propertyName, val);
    }
  }

  private static void set(DesiredCapabilities capabilities, PropertyName name, ResponseInterceptor[] val) {
    if (val != null && val.length > 0) {
      ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
      try (ObjectOutputStream objectOut = new ObjectOutputStream(streamOut)) {
        objectOut.writeObject(val);
      } catch (Throwable t) {
        Util.handleException(t);
      }
      capabilities.setCapability(name.propertyName, Base64.getEncoder().encodeToString(streamOut.toByteArray()));
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

  private static ResponseInterceptor[] parse(Map capabilities, PropertyName name, ResponseInterceptor[] fallback) {
    if (capabilities.get(name.propertyName) != null) {
      BufferedInputStream bufferIn = new BufferedInputStream(new ByteArrayInputStream(
          Base64.getDecoder().decode(capabilities.get(name.propertyName).toString())));
      try (ObjectInputStream objectIn = new ObjectInputStream(bufferIn)) {
        return (ResponseInterceptor[]) objectIn.readObject();
      } catch (Throwable t) {
        Util.handleException(t);
      }
    }
    return fallback;
  }

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
  private final List<Integer> childPorts;
  private final int parentPort;
  private final boolean headless;
  private final long ajaxWait;
  private final long ajaxResourceTimeout;
  private final boolean blockAds;
  private final boolean quickRender;
  private final int maxRouteConnections;
  private final int maxConnections;
  private final String ssl;
  private final boolean wireLog;
  private final boolean javascriptLog;
  private final boolean traceLog;
  private final boolean warnLog;
  private final boolean wireConsole;
  private final boolean javascriptConsole;
  private final boolean traceConsole;
  private final boolean warnConsole;
  private final int maxLogs;
  private final boolean hostnameVerification;
  private final boolean javascript;
  private final int socketTimeout;
  private final int connectTimeout;
  private final int connectionReqTimeout;
  private final String host;
  //TODO private final ResponseInterceptor[] responseInterceptors;

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
    this.javascriptLog = parse(properties, PropertyName.JAVASCRIPT_LOG, builder.javascriptLog);
    this.wireLog = parse(properties, PropertyName.WIRE_LOG, builder.wireLog);
    this.traceLog = parse(properties, PropertyName.TRACE_LOG, builder.traceLog);
    this.warnLog = parse(properties, PropertyName.WARN_LOG, builder.warnLog);
    this.javascriptConsole = parse(properties, PropertyName.JAVASCRIPT_CONSOLE, builder.javascriptConsole);
    this.wireConsole = parse(properties, PropertyName.WIRE_CONSOLE, builder.wireConsole);
    this.traceConsole = parse(properties, PropertyName.TRACE_CONSOLE, builder.traceConsole);
    this.warnConsole = parse(properties, PropertyName.WARN_CONSOLE, builder.warnConsole);
    this.maxLogs = parse(properties, PropertyName.MAX_LOGS, builder.maxLogs);
    this.hostnameVerification = parse(properties, PropertyName.HOSTNAME_VERIFICATION, builder.hostnameVerification);
    this.javascript = parse(properties, PropertyName.JAVASCRIPT, builder.javascript);
    this.socketTimeout = parse(properties, PropertyName.SOCKET_TIMEOUT_MS, builder.socketTimeout);
    this.connectTimeout = parse(properties, PropertyName.CONNECT_TIMEOUT_MS, builder.connectTimeout);
    this.connectionReqTimeout = parse(properties, PropertyName.CONNECTION_REQ_TIMEOUT_MS, builder.connectionReqTimeout);
    //TODO this.responseInterceptors = parse(properties, PropertyName.RESPONSE_INTERCEPTORS, builder.responseInterceptors);
    this.cacheDir = properties.get(PropertyName.CACHE_DIR.propertyName) == null
        ? builder.cacheDir : new File(properties.get(PropertyName.CACHE_DIR.propertyName).toString());
    this.host = parse(properties, PropertyName.HOST, builder.host);
    if (properties.get(PropertyName.PORTS.propertyName) == null
        && properties.get(PropertyName.PORT_RANGES.propertyName) == null
        && properties.get(PropertyName.PROCESSES.propertyName) == null) {
      if (builder.processes > -1) {
        this.childPorts = parseProcesses(Integer.toString(builder.processes));
        this.parentPort = childPorts.remove(0);
      } else {
        this.childPorts = new ArrayList<Integer>(builder.ports);
        this.parentPort = childPorts.remove(0);
      }
    } else if (properties.get(PropertyName.PORTS.propertyName) != null) {
      System.err.println("jBrowserDriver: The jbd.ports property is deprecated and will be removed. Refer to Settings.Builder.processes(..) API documentation.");
      this.childPorts = parsePorts(properties.get(PropertyName.PORTS.propertyName).toString());
      this.parentPort = childPorts.remove(0);
    } else if (properties.get(PropertyName.PORT_RANGES.propertyName) != null) {
      this.childPorts = parsePorts(properties.get(PropertyName.PORT_RANGES.propertyName).toString());
      this.parentPort = childPorts.remove(0);
    } else {
      this.childPorts = parseProcesses(properties.get(PropertyName.PROCESSES.propertyName).toString());
      this.parentPort = childPorts.remove(0);
    }

    //backwards compatible property name for versions <= 0.9.1
    boolean headlessTmp = parse(properties, PropertyName.HEADLESS, builder.headless);
    headlessTmp = System.getProperty(PropertyName.HEADLESS.propertyName) == null
        && System.getProperty("jbd.browsergui") != null
            ? !Boolean.parseBoolean(System.getProperty("jbd.browsergui")) : headlessTmp;
    this.headless = headlessTmp;
    if (System.getProperty("jbd.browsergui") != null) {
      System.err.println("jBrowserDriver: The jbd.browsergui property is deprecated and will be removed. Use jbd.headless property instead.");
    }

    //backwards compatible property name for versions <= 0.9.1
    String sslTmp = parse(properties, PropertyName.SSL, builder.ssl);
    sslTmp = System.getProperty(PropertyName.SSL.propertyName) == null
        && System.getProperty("jbd.pemfile") != null
            ? System.getProperty("jbd.pemfile") : sslTmp;
    this.ssl = sslTmp;
    if (System.getProperty("jbd.pemfile") != null) {
      System.err.println("jBrowserDriver: The jbd.pemfile property is deprecated and will be removed. Use jbd.ssl property instead.");
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

  Collection<Integer> childPorts() {
    return childPorts;
  }

  int parentPort() {
    return parentPort;
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

  boolean wireConsole() {
    return wireConsole;
  }

  boolean javascriptConsole() {
    return javascriptConsole;
  }

  boolean traceConsole() {
    return traceConsole;
  }

  boolean warnConsole() {
    return warnConsole;
  }

  boolean wireLog() {
    return wireLog;
  }

  boolean javascriptLog() {
    return javascriptLog;
  }

  boolean traceLog() {
    return traceLog;
  }

  boolean warnLog() {
    return warnLog;
  }

  int maxLogs() {
    return maxLogs;
  }

  boolean hostnameVerification() {
    return hostnameVerification;
  }

  //TODO
  //  ResponseInterceptor[] responseInterceptors() {
  //    return responseInterceptors;
  //  }
}
