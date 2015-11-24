/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Browser user agent and related properties.
 * 
 * @see RequestHeaders
 */
public class UserAgent {
  public static enum Family {
    /**
     * Firefox and Tor browsers
     */
    MOZILLA,

    /**
     * Safari and Chrome browsers
     */
    WEBKIT
  }

  private static final String baseNavigator;

  static {
    Map<String, String> baseNavigatorTmp = new LinkedHashMap<String, String>();
    baseNavigatorTmp.put("vibrate", "function(){return true;}");
    baseNavigatorTmp.put("vibrate.toString", "function(){return 'function vibrate() { [native code] }';}");
    baseNavigatorTmp.put("javaEnabled", "function(){return false;}");
    baseNavigatorTmp.put("javaEnabled.toString", "function(){return 'function javaEnabled() { [native code] }';}");
    baseNavigatorTmp.put("sendBeacon", "function(){return false;}");
    baseNavigatorTmp.put("sendBeacon.toString", "function(){return 'function sendBeacon() { [native code] }';}");
    baseNavigatorTmp.put("registerProtocolHandler", "function(){}");
    baseNavigatorTmp.put("registerProtocolHandler.toString", "function(){return 'function registerProtocolHandler() { [native code] }';}");
    baseNavigatorTmp.put("registerContentHandler", "function(){}");
    baseNavigatorTmp.put("registerContentHandler.toString", "function(){return 'function registerContentHandler() { [native code] }';}");
    baseNavigatorTmp.put("taintEnabled", "function(){return false;}");
    baseNavigatorTmp.put("taintEnabled.toString", "function(){return 'function taintEnabled() { [native code] }';}");
    baseNavigatorTmp.put("mimeTypes", "navigator.mimeTypes");
    baseNavigatorTmp.put("mimeTypes.toString", "function(){return '[object MimeTypeArray]';}");
    baseNavigatorTmp.put("plugins", "navigator.plugins");
    baseNavigatorTmp.put("plugins.toString", "function(){return '[object PluginArray]';}");
    baseNavigatorTmp.put("doNotTrack", "'unspecified'");
    baseNavigatorTmp.put("cookieEnabled", "true");
    baseNavigatorTmp.put("onLine", "true");
    baseNavigator = buildNavigator(baseNavigatorTmp);
  }

  private static final String mozNavigator;

  static {
    Map<String, String> mozNavigatorTmp = new LinkedHashMap<String, String>();
    mozNavigatorTmp.put("mozId", "null");
    mozNavigatorTmp.put("mozPay", "null");
    mozNavigatorTmp.put("mozAlarms", "null");
    mozNavigatorTmp.put("mozContacts", "{toString:function(){return '[object ContactManager]';},"
        + "find:function(){return new Object();},"
        + "getAll:function(){return new Object();},"
        + "clear:function(){return new Object();},"
        + "save:function(){return new Object();},"
        + "remove:function(){return new Object();},"
        + "getRevision:function(){return new Object();},"
        + "getCount:function(){return new Object();},"
        + "oncontactchange:null,"
        + "addEventListener:function(){return new Object();},"
        + "removeEventListener:function(){return new Object();},"
        + "dispatchEvent:function(){return new Object();},"
        + "}");
    mozNavigatorTmp.put("mozPhoneNumberService", "''");
    mozNavigatorTmp.put("mozApps", "{toString:function(){return '[xpconnect wrapped (nsISupports, mozIDOMApplicationRegistry, mozIDOMApplicationRegistry2)]';},"
        + "QueryInterface:function(){return new Object();},"
        + "install:function(){return new Object();},"
        + "getSelf:function(){return new Object();},"
        + "checkInstalled:function(){return new Object();},"
        + "getInstalled:function(){return new Object();},"
        + "installPackage:function(){return new Object();},"
        + "mgmt:null,"
        + "}");
    mozNavigatorTmp.put("mozTCPSocket", "null");
    mozNavigatorTmp.put("mozIsLocallyAvailable", "function(){return false;}");
    mozNavigatorTmp.put("mozIsLocallyAvailable.toString", "function(){return 'function mozIsLocallyAvailable() { [native code] }';}");

    mozNavigatorTmp.put("vendorSub", "''");
    mozNavigatorTmp.put("productSub", "'20100101'");
    mozNavigatorTmp.put("buildID", "'20100101'");
    mozNavigatorTmp.put("appCodeName", "'Mozilla'");
    mozNavigatorTmp.put("appName", "'Netscape'");
    mozNavigatorTmp.put("product", "'Gecko'");
    mozNavigator = buildNavigator(mozNavigatorTmp);
  }

  private static final String webkitNavigator;

  static {
    Map<String, String> webkitNavigatorTmp = new LinkedHashMap<String, String>();
    webkitNavigatorTmp.put("vendorSub", "''");
    webkitNavigatorTmp.put("productSub", "'20030107'");
    webkitNavigatorTmp.put("buildID", "'20030107'");
    webkitNavigatorTmp.put("appCodeName", "'Mozilla'");
    webkitNavigatorTmp.put("appName", "'Netscape'");
    webkitNavigatorTmp.put("product", "'Gecko'");
    webkitNavigator = buildNavigator(webkitNavigatorTmp);
  }

  /**
   * Tor Browser
   * 
   * @see RequestHeaders#TOR
   */
  public static UserAgent TOR = new UserAgent(
      Family.MOZILLA, "", "Win32", "Windows NT 6.1",
      "5.0 (Windows)",
      "Mozilla/5.0 (Windows NT 6.1; rv:38.0) Gecko/20100101 Firefox/38.0");

  /**
   * Chrome browser
   * 
   * @see RequestHeaders#CHROME
   */
  public static UserAgent CHROME = new UserAgent(
      Family.WEBKIT, "Google Inc.", "Win32", "Windows NT 6.1",
      "5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36",
      "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36");

  private final String script;
  private final String userAgentString;

  /**
   * @param family
   *          Category of browsers which share similar properties
   * @param vendor
   *          Property of the navigator object
   * @param platform
   *          Property of the navigator object
   * @param oscpu
   *          Property of the navigator object
   * @param appVersion
   *          Property of the navigator object
   * @param userAgent
   *          Property of the navigator object and also sent on headers
   */
  public UserAgent(Family family, String vendor, String platform,
      String oscpu, String appVersion, String userAgentString) {
    StringBuilder builder = new StringBuilder();
    builder.append(baseNavigator);
    if (family == Family.MOZILLA) {
      builder.append(mozNavigator);
    } else if (family == Family.WEBKIT) {
      builder.append(webkitNavigator);
    }
    Map<String, String> navigator = new LinkedHashMap<String, String>();
    navigator.put("language", "'en-US'");//TODO support others
    navigator.put("vendor", "'" + (vendor == null ? "" : vendor) + "'");
    navigator.put("platform", "'" + (platform == null ? "" : platform) + "'");
    navigator.put("oscpu", "'" + (oscpu == null ? "" : oscpu) + "'");
    navigator.put("appVersion", "'" + (appVersion == null ? "" : appVersion) + "'");
    navigator.put("userAgent", "'" + (userAgentString == null ? "" : userAgentString) + "'");
    builder.append(buildNavigator(navigator));

    this.userAgentString = userAgentString;
    script = builder.toString();
  }

  private static String buildNavigator(Map<String, String> navigator) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> entry : navigator.entrySet()) {
      if (entry.getKey().contains(".")) {
        int lastDot = entry.getKey().lastIndexOf(".");
        String parent = entry.getKey().substring(0, lastDot);
        String child = entry.getKey().substring(lastDot + 1);
        builder.append("Object.defineProperty(window.navigator." + parent + ",'" + child + "',");
        builder.append("{value:" + entry.getValue() + "});");
      } else {
        builder.append("Object.defineProperty(window.navigator,'" + entry.getKey() + "',{value:");
        builder.append(entry.getValue());
        builder.append("});");
      }
    }
    return builder.toString();
  }

  String userAgentString() {
    return userAgentString;
  }

  String script() {
    return script;
  }
}
