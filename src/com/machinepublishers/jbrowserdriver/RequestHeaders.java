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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Customizes headers sent on each request.
 * This can be used to mimic behavior of particular browsers
 * or potentially set up more customized caching and authentication
 * settings.
 * 
 * @see UserAgent
 */
public class RequestHeaders implements Serializable {

  private final LinkedHashMap<String, String> headersHttp;
  private final LinkedHashMap<String, String> headersHttps;
  /**
   * Use this as a header value to force the header to be dropped from the request. For instance,
   * JavaFX WebKit will always add the Cookie header but adding the header map entry
   * &lt;"Cookie", RequestHeaders.DROP_HEADER&gt; will force the header to not be sent.
   */
  public static final String DROP_HEADER = "drop_header";
  /**
   * Use this as a header value to force the header to be replaced by a value generated at runtime
   * by the browser engine. For instance, JavaFX WebKit will always add Host header. Adding the
   * header map entry &lt;"Host", RequestHeaders.DYNAMIC_HEADER&gt; will preserve its ordering in
   * the headers map you specify, but its value will be decided at runtime for each request.
   */
  public static final String DYNAMIC_HEADER = "dynamic_header";

  /**
   * Tor Browser request headers
   * 
   * @see UserAgent#TOR
   */
  public static final RequestHeaders TOR;

  static {
    LinkedHashMap<String, String> headersTmp = new LinkedHashMap<String, String>();
    headersTmp.put("Host", DYNAMIC_HEADER);
    headersTmp.put("User-Agent", DYNAMIC_HEADER);
    headersTmp.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    headersTmp.put("Accept-Language", "en-us,en;q=0.5");
    headersTmp.put("Accept-Encoding", "gzip, deflate");
    headersTmp.put("Cookie", DYNAMIC_HEADER);
    headersTmp.put("DNT", "1");
    headersTmp.put("Connection", "keep-alive");
    TOR = new RequestHeaders(headersTmp);
  }

  /**
   * Chrome browser request headers
   * 
   * @see UserAgent#CHROME
   */
  public static final RequestHeaders CHROME;

  static {
    LinkedHashMap<String, String> headersTmp = new LinkedHashMap<String, String>();
    headersTmp.put("Host", DYNAMIC_HEADER);
    headersTmp.put("Connection", "keep-alive");
    headersTmp.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    headersTmp.put("Upgrade-Insecure-Requests", "1");
    headersTmp.put("User-Agent", DYNAMIC_HEADER);
    headersTmp.put("Accept-Encoding", "gzip, deflate, sdch");
    headersTmp.put("Accept-Language", "en-US,en;q=0.8");
    headersTmp.put("Cookie", DYNAMIC_HEADER);

    CHROME = new RequestHeaders(headersTmp);
  }

  /**
   * Specify the ordered headers to be sent on each request.
   * 
   * @see RequestHeaders.DROP_HEADER
   * @see RequestHeaders.DYNAMIC_HEADER
   */
  public RequestHeaders(LinkedHashMap<String, String> headers) {
    headersHttp = createHeaders(headers);
    headersHttps = headersHttp;
  }

  /**
   * Specify the ordered headers to be sent on each request.
   * Allows different sets of headers for HTTP and HTTPS.
   * 
   * @see RequestHeaders.DROP_HEADER
   * @see RequestHeaders.DYNAMIC_HEADER
   */
  public RequestHeaders(LinkedHashMap<String, String> headersHttp, LinkedHashMap<String, String> headersHttps) {
    this.headersHttp = createHeaders(headersHttp);
    this.headersHttps = createHeaders(headersHttps);
  }

  private static LinkedHashMap<String, String> createHeaders(LinkedHashMap<String, String> headersIn) {
    LinkedHashMap<String, String> headersOut = new LinkedHashMap<String, String>(headersIn);
    if (!headersOut.containsKey("Accept-Charset")
        && !headersOut.containsKey("accept-charset")
        && !headersOut.containsKey("Accept-charset")
        && !headersOut.containsKey("ACCEPT-CHARSET")) {
      headersOut.put("Accept-Charset", DROP_HEADER);
    }
    if (!headersOut.containsKey("Pragma")
        && !headersOut.containsKey("pragma")
        && !headersOut.containsKey("PRAGMA")) {
      headersOut.put("Pragma", DROP_HEADER);
    }
    return headersOut;
  }

  Collection<String> namesHttp() {
    return headersHttp.keySet();
  }

  String headerHttp(String name) {
    return headersHttp.get(name);
  }

  Collection<String> namesHttps() {
    return headersHttps.keySet();
  }

  String headerHttps(String name) {
    return headersHttps.get(name);
  }
}
