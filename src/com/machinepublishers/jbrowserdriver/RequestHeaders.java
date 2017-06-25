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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
  private final Map<String, String> headersHttpCasing;
  private final LinkedHashMap<String, String> headersHttps;
  private final Map<String, String> headersHttpsCasing;
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
    headersTmp.put("Referer", DYNAMIC_HEADER);
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
    headersTmp.put("Referer", DYNAMIC_HEADER);
    headersTmp.put("Accept-Encoding", "gzip, deflate, sdch");
    headersTmp.put("Accept-Language", "en-US,en;q=0.8");
    headersTmp.put("Cookie", DYNAMIC_HEADER);

    CHROME = new RequestHeaders(headersTmp);
  }

  /**
   * Specify the ordered headers to be sent on each request.
   * 
   * @see RequestHeaders#DROP_HEADER
   * @see RequestHeaders#DYNAMIC_HEADER
   */
  public RequestHeaders(LinkedHashMap<String, String> headers) {
    this(headers, headers);
  }

  /**
   * Specify the ordered headers to be sent on each request.
   * Allows different sets of headers for HTTP and HTTPS.
   * 
   * @see RequestHeaders#DROP_HEADER
   * @see RequestHeaders#DYNAMIC_HEADER
   */
  public RequestHeaders(LinkedHashMap<String, String> headersHttp, LinkedHashMap<String, String> headersHttps) {
    LinkedHashMap<String, String> headersHttpTmp = new LinkedHashMap<String, String>();
    Map<String, String> headersHttpCasingTmp = new HashMap<String, String>();
    LinkedHashMap<String, String> headersHttpsTmp = new LinkedHashMap<String, String>();
    Map<String, String> headersHttpsCasingTmp = new HashMap<String, String>();
    createHeaders(headersHttp, headersHttpTmp, headersHttpCasingTmp);
    createHeaders(headersHttps, headersHttpsTmp, headersHttpsCasingTmp);
    this.headersHttp = headersHttpTmp;
    this.headersHttpCasing = headersHttpCasingTmp;
    this.headersHttps = headersHttpsTmp;
    this.headersHttpsCasing = headersHttpsCasingTmp;
  }

  private static void createHeaders(
      LinkedHashMap<String, String> headersIn,
      LinkedHashMap<String, String> headersOut,
      Map<String, String> headersCasing) {
    for (Map.Entry<String, String> cur : headersIn.entrySet()) {
      headersOut.put(cur.getKey().toLowerCase(), cur.getValue());
      headersCasing.put(cur.getKey().toLowerCase(), cur.getKey());
    }
    if (!headersOut.containsKey("accept-charset")) {
      headersOut.put("accept-charset", DROP_HEADER);
      headersCasing.put("accept-charset", "Accept-Charset");
    }
    if (!headersOut.containsKey("pragma")) {
      headersOut.put("pragma", DROP_HEADER);
      headersCasing.put("pragma", "Pragma");
    }
  }

  String nameFromLowercase(String headerNameLowercase, boolean https) {
    return https ? headersHttpsCasing.get(headerNameLowercase) : headersHttpCasing.get(headerNameLowercase);
  }

  Collection<String> headerNames(boolean https) {
    return https ? headersHttps.keySet() : headersHttp.keySet();
  }

  String headerValue(String name, boolean https) {
    return https ? headersHttps.get(name) : headersHttp.get(name);
  }
}
