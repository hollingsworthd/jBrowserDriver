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
package com.machinepublishers.jbrowserdriver.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class RequestHeaders {
  private final Map<String, String> headers;
  private static final Collection<String> defaultHeaders = Collections.unmodifiableSet(
      new HashSet<String>(Arrays.asList(new String[] {
          "Accept", "User-Agent", "Accept-Encoding", "Accept-Language", "Accept-Charset"
      })));

  public RequestHeaders() {
    Map<String, String> headersTmp = new LinkedHashMap<String, String>();
    headersTmp.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    headersTmp.put("User-Agent",
        "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2114.2 Safari/537.36");
    headersTmp.put("Accept-Encoding", "gzip,deflate");
    headersTmp.put("Accept-Language", "en-US,en;q=0.5");
    headersTmp.put("Accept-Charset", "");
    headers = Collections.unmodifiableMap(headersTmp);
  }

  public RequestHeaders(Map<String, String> headers) {
    this.headers = Collections.unmodifiableMap(headers);
  }

  Collection<String> names() {
    return headers.keySet();
  }

  String header(String name) {
    return headers.get(name);
  }

  static Collection<String> defaultHeaders() {
    return defaultHeaders;
  }
}
