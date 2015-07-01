/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * jBrowserDriver is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking jBrowserDriver statically or dynamically with other modules is making a combined work
 *   based on jBrowserDriver. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of jBrowserDriver with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on jBrowserDriver. If you modify jBrowserDriver, you may not
 *   extend this exception to your modified version of jBrowserDriver.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openqa.selenium.Dimension;

/**
 * Specifies various DOM and JavaScript properties (i.e., browser fingerprint).
 */
public class BrowserProperties {
  private final String script;
  private final Dimension size;

  /**
   * Set properties similar to Tor Browser's.
   */
  public BrowserProperties() {
    this(null, null, null, null, null);
  }

  public static final String AUTO_SIZE = "auto_size";

  /**
   * Leave any parameter as null to accept the default value (which mimics Tor Browser).
   * 
   * @param canvasProtection
   *          Whether to disable canvas fingerprinting.
   * @param size
   *          Size of the screen, which is applied to some window and window.screen properties.
   * @param screen
   *          Screen properties. Use BrowserProperties.AUTO_SIZE for map-value to automatically
   *          apply given size parameter. Also, String values must be enclosed in single quotes
   *          to be recognized as strings in JavaScript (otherwise they'll be interpreted as
   *          boolean, number, null, undefined, or function). To set grandchildren properties
   *          (e.g., screen.someChild.someGrandchild) use a key value with dots (e.g.,
   *          someChild.someGrandchild). To only set the child property, the key value would be,
   *          e.g., someChild
   * @param navigator
   *          Navigator properties. Used in the same manner as 'screen' parameter above.
   * @param supplementaryJS
   *          Arbitary javascript to be executed.
   */
  public BrowserProperties(Boolean canvasProtection, Dimension size, LinkedHashMap<String, String> screen,
      LinkedHashMap<String, String> navigator, String supplementaryJS) {
    this.size = size == null ? new Dimension(1366, 768) : size;
    StringBuilder builder = new StringBuilder();
    if (canvasProtection == null || canvasProtection) {
      buildCanvasProtection(builder);
    }
    buildSize(builder, this.size);
    buildScreen(builder, screen == null ? defaultScreen(this.size) : screen, this.size);
    buildNavigator(builder, navigator == null ? defaultNavigator() : navigator);
    builder.append(supplementaryJS == null ? defaultSupplementaryJS() : supplementaryJS);
    this.script = builder.toString();
  }

  private static LinkedHashMap<String, String> defaultScreen(Dimension size) {
    LinkedHashMap<String, String> screen = new LinkedHashMap<String, String>();
    screen.put("mozLockOrientation", "function(){}");
    screen.put("mozLockOrientation.toString", "function(){return '{ [native code] }';}");
    screen.put("mozUnlockOrientation", "function(){}");
    screen.put("mozUnlockOrientation.toString", "function(){return '{ [native code] }';}");
    screen.put("availWidth", Integer.toString(size.getWidth()));
    screen.put("availHeight", Integer.toString(size.getHeight()));
    screen.put("width", Integer.toString(size.getWidth()));
    screen.put("height", Integer.toString(size.getHeight()));
    screen.put("colorDepth", "24");
    screen.put("pixelDepth", "24");
    screen.put("top", "0");
    screen.put("left", "0");
    screen.put("availTop", "0");
    screen.put("availLeft", "0");
    screen.put("mozOrientation", "'landscape-primary'");
    screen.put("onmozorientationchange", "null");
    screen.put("addEventListener", "function(){}");
    screen.put("addEventListener.toString", "function(){return '{ [native code] }';}");
    screen.put("removeEventListener", "function(){}");
    screen.put("removeEventListener.toString", "function(){return '{ [native code] }';}");
    screen.put("dispatchEvent", "function(){}");
    screen.put("dispatchEvent.toString", "function(){return '{ [native code] }';}");
    return screen;
  }

  private static LinkedHashMap<String, String> defaultNavigator() {
    LinkedHashMap<String, String> navigator = new LinkedHashMap<String, String>();
    navigator.put("mozId", "null");
    navigator.put("mozPay", "null");
    navigator.put("mozAlarms", "null");
    navigator.put("mozContacts", "{toString:function(){return '[object ContactManager]';},"
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
    navigator.put("mozPhoneNumberService", "''");
    navigator.put("mozApps", "{toString:function(){return '[xpconnect wrapped (nsISupports, mozIDOMApplicationRegistry, mozIDOMApplicationRegistry2)]';},"
        + "QueryInterface:function(){return new Object();},"
        + "install:function(){return new Object();},"
        + "getSelf:function(){return new Object();},"
        + "checkInstalled:function(){return new Object();},"
        + "getInstalled:function(){return new Object();},"
        + "installPackage:function(){return new Object();},"
        + "mgmt:null,"
        + "}");
    navigator.put("mozTCPSocket", "null");
    navigator.put("vibrate", "function(){return true;}");
    navigator.put("vibrate.toString", "function(){return 'function vibrate() { [native code] }';}");
    navigator.put("javaEnabled", "function(){return false;}");
    navigator.put("javaEnabled.toString", "function(){return 'function javaEnabled() { [native code] }';}");
    navigator.put("mozIsLocallyAvailable", "function(){return false;}");
    navigator.put("mozIsLocallyAvailable.toString", "function(){return 'function mozIsLocallyAvailable() { [native code] }';}");
    navigator.put("sendBeacon", "function(){return false;}");
    navigator.put("sendBeacon.toString", "function(){return 'function sendBeacon() { [native code] }';}");
    navigator.put("registerProtocolHandler", "function(){}");
    navigator.put("registerProtocolHandler.toString", "function(){return 'function registerProtocolHandler() { [native code] }';}");
    navigator.put("registerContentHandler", "function(){}");
    navigator.put("registerContentHandler.toString", "function(){return 'function registerContentHandler() { [native code] }';}");
    navigator.put("taintEnabled", "function(){return false;}");
    navigator.put("taintEnabled.toString", "function(){return 'function taintEnabled() { [native code] }';}");
    navigator.put("mimeTypes", "navigator.mimeTypes");
    navigator.put("mimeTypes.toString", "function(){return '[object MimeTypeArray]';}");
    navigator.put("plugins", "navigator.plugins");
    navigator.put("plugins.toString", "function(){return '[object PluginArray]';}");
    navigator.put("doNotTrack", "'unspecified'");
    navigator.put("oscpu", "'Windows NT 6.1'");
    navigator.put("vendor", "''");
    navigator.put("vendorSub", "''");
    navigator.put("productSub", "'20100101'");
    navigator.put("cookieEnabled", "true");
    navigator.put("buildID", "'20100101'");
    navigator.put("appCodeName", "'Mozilla'");
    navigator.put("appName", "'Netscape'");
    navigator.put("appVersion", "'5.0 (Windows)'");
    navigator.put("platform", "'Win32'");
    navigator.put("userAgent", "'Mozilla/5.0 (Windows NT 6.1; rv:31.0) Gecko/20100101 Firefox/31.0'");
    navigator.put("product", "'Gecko'");
    navigator.put("language", "'en-US'");
    navigator.put("onLine", "true");
    return navigator;
  }

  private static String defaultSupplementaryJS() {
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
    return builder.toString();
  }

  private static void buildCanvasProtection(StringBuilder builder) {
    builder.append("Object.defineProperty(HTMLCanvasElement.prototype, "
        + "'toBlob', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(HTMLCanvasElement.prototype, "
        + "'toDataURL', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
        + "'createImageData', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
        + "'getImageData', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
        + "'measureText', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
        + "'isPointInPath', {value:function(){return undefined;}});");
    builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
        + "'isPointInStroke', {value:function(){return undefined;}});");
  }

  private static void buildSize(StringBuilder builder, Dimension size) {
    builder.append("Object.defineProperty(window,'outerWidth',{value:" + size.getWidth() + "});");
    builder.append("Object.defineProperty(window,'outerHeight',{value:" + size.getHeight() + "});");
    builder.append("Object.defineProperty(window,'innerWidth',{value:" + size.getWidth() + "});");
    builder.append("Object.defineProperty(window,'innerHeight',{value:" + size.getHeight() + "});");
    builder.append("Object.defineProperty(window,'screenX',{value: 0});");
    builder.append("Object.defineProperty(window,'screenY',{value: 0});");
  }

  private static void buildScreen(StringBuilder builder, LinkedHashMap<String, String> screen, Dimension size) {
    builder.append("Object.defineProperty(window,'screen',");
    builder.append("{value:{");
    for (Map.Entry<String, String> entry : screen.entrySet()) {
      boolean added = false;
      if (entry.getValue() == AUTO_SIZE) {
        added = true;
        if (size != null
            && (entry.getKey().equals("width") || entry.getKey().equals("availWidth"))) {
          builder.append(entry.getKey());
          builder.append(": ");
          builder.append(size.getWidth());
          builder.append(",");
        } else if (size != null
            && (entry.getKey().equals("height") || entry.getKey().equals("availHeight"))) {
          builder.append(entry.getKey());
          builder.append(": ");
          builder.append(size.getHeight());
          builder.append(",");
        } else if (entry.getKey().equals("availLeft") || entry.getKey().equals("availTop")
            || entry.getKey().equals("left") || entry.getKey().equals("top")) {
          builder.append(entry.getKey());
          builder.append(": 0,");
        } else if (entry.getKey().equals("pixelDepth") || entry.getKey().equals("colorDepth")) {
          builder.append(entry.getKey());
          builder.append(": 24,");
        } else {
          added = false;
        }
      }
      if (!added && !entry.getKey().contains(".")) {
        builder.append(entry.getKey());
        builder.append(": ");
        builder.append(entry.getValue());
        builder.append(",");
      }
    }
    builder.append("}});");
    for (Map.Entry<String, String> entry : screen.entrySet()) {
      if (entry.getKey().contains(".")) {
        int lastDot = entry.getKey().lastIndexOf(".");
        String parent = entry.getKey().substring(0, lastDot);
        String child = entry.getKey().substring(lastDot + 1);
        builder.append("Object.defineProperty(window.screen." + parent + ",'" + child + "',");
        builder.append("{value:" + entry.getValue() + "});");
      }
    }
  }

  private static void buildNavigator(StringBuilder builder, LinkedHashMap<String, String> navigator) {
    builder.append("Object.defineProperty(window,'navigator',");
    builder.append("{value:{");
    for (Map.Entry<String, String> entry : navigator.entrySet()) {
      if (!entry.getKey().contains(".")) {
        builder.append(entry.getKey());
        builder.append(": ");
        builder.append(entry.getValue());
        builder.append(",");
      }
    }
    builder.append("}});");
    for (Map.Entry<String, String> entry : navigator.entrySet()) {
      if (entry.getKey().contains(".")) {
        int lastDot = entry.getKey().lastIndexOf(".");
        String parent = entry.getKey().substring(0, lastDot);
        String child = entry.getKey().substring(lastDot + 1);
        builder.append("Object.defineProperty(window.navigator." + parent + ",'" + child + "',");
        builder.append("{value:" + entry.getValue() + "});");
      }
    }
  }

  Dimension size() {
    return size;
  }

  String script() {
    return script;
  }
}
