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
    mozNavigatorTmp.put("mozContacts", new StringBuilder()
        .append("{toString:function(){return '[object ContactManager]';},")
        .append("find:function(){return new Object();},")
        .append("getAll:function(){return new Object();},")
        .append("clear:function(){return new Object();},")
        .append("save:function(){return new Object();},")
        .append("remove:function(){return new Object();},")
        .append("getRevision:function(){return new Object();},")
        .append("getCount:function(){return new Object();},")
        .append("oncontactchange:null,")
        .append("addEventListener:function(){return new Object();},")
        .append("removeEventListener:function(){return new Object();},")
        .append("dispatchEvent:function(){return new Object();},")
        .append("}").toString());
    mozNavigatorTmp.put("mozPhoneNumberService", "''");
    mozNavigatorTmp.put("mozApps", new StringBuilder()
        .append("{toString:function(){return '[xpconnect wrapped (nsISupports, mozIDOMApplicationRegistry, mozIDOMApplicationRegistry2)]';},")
        .append("QueryInterface:function(){return new Object();},")
        .append("install:function(){return new Object();},")
        .append("getSelf:function(){return new Object();},")
        .append("checkInstalled:function(){return new Object();},")
        .append("getInstalled:function(){return new Object();},")
        .append("installPackage:function(){return new Object();},")
        .append("mgmt:null,")
        .append("}").toString());
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
   * @param userAgentString
   *          Property of the navigator object and also sent on headers
   */
  public UserAgent(Family family, String vendor, String platform,
      String oscpu, String appVersion, String userAgentString) {
    this(family, "en-US", vendor, platform, oscpu, appVersion, userAgentString);
  }

  /**
   * @param family
   *          Category of browsers which share similar properties
   * @param language
   *          Language property of the navigator object
   * @param vendor
   *          Property of the navigator object
   * @param platform
   *          Property of the navigator object
   * @param oscpu
   *          Property of the navigator object
   * @param appVersion
   *          Property of the navigator object
   * @param userAgentString
   *          Property of the navigator object and also sent on headers
   */
  public UserAgent(Family family, String language, String vendor, String platform,
      String oscpu, String appVersion, String userAgentString) {
    StringBuilder builder = new StringBuilder();
    builder.append(baseNavigator);
    if (family == Family.MOZILLA) {
      builder.append(mozNavigator);
    } else if (family == Family.WEBKIT) {
      builder.append(webkitNavigator);
    }
    Map<String, String> navigator = new LinkedHashMap<String, String>();
    navigator.put("language", "'" + (language == null ? "" : language) + "'");
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
        builder.append("Object.defineProperty(window.navigator.").append(parent).append(",'").append(child).append("',");
        builder.append("{value:").append(entry.getValue()).append("});");
      } else {
        builder.append("try{");
        builder.append("Object.defineProperty(window.navigator,'").append(entry.getKey());
        builder.append("',{value:").append(entry.getValue());
        builder.append("});");
        builder.append("}catch(e){");
        builder.append("window.navigator = Object.create(navigator, {" + entry.getKey());
        builder.append(":{value:").append(entry.getValue()).append("}");
        builder.append("});");
        builder.append("}");
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
