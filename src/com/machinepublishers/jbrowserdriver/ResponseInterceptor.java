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

import java.io.Serializable;
import java.net.HttpURLConnection;

/**
 * Intercepts a response and allows modification of it before it's passed to the browser.
 */
public interface ResponseInterceptor extends Serializable {
  /**
   * Intercept the response and allow modification of the response body or headers.
   * The content-length header will be updated automatically.
   * 
   * @param connection
   *          Connection to intercept. This is the same connection passed to the browser next,
   *          so modifying its properties will have an effect.
   * @param inflatedContent
   *          Unzipped/inflated response body
   * @param originalUrl
   *          URL before any redirects
   * @return Content to replace the <code>inflatedContent</code> passed in or <code>null</code> to not modify the content
   *         or <code>byte[0]</code> to send empty content to the browser. Do not zip/deflate the return value.
   */
  byte[] intercept(final HttpURLConnection connection, byte[] inflatedContent, String originalUrl);
}