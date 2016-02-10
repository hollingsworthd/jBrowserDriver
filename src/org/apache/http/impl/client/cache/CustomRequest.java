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
package org.apache.http.impl.client.cache;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.params.HttpParams;

public class CustomRequest implements HttpRequest {

  private final HttpRequest req;

  public CustomRequest(HttpRequest req) {
    this.req = req;
  }

  @Override
  public ProtocolVersion getProtocolVersion() {
    return req.getProtocolVersion();
  }

  @Override
  public boolean containsHeader(String name) {
    removeHeaders("Via");
    return req.containsHeader(name);
  }

  @Override
  public Header[] getHeaders(String name) {
    removeHeaders("Via");
    return req.getHeaders(name);
  }

  @Override
  public Header getFirstHeader(String name) {
    removeHeaders("Via");
    return req.getFirstHeader(name);
  }

  @Override
  public Header getLastHeader(String name) {
    removeHeaders("Via");
    return req.getLastHeader(name);
  }

  @Override
  public Header[] getAllHeaders() {
    removeHeaders("Via");
    return req.getAllHeaders();
  }

  @Override
  public void addHeader(Header header) {
    req.addHeader(header);
  }

  @Override
  public void addHeader(String name, String value) {
    req.addHeader(name, value);
  }

  @Override
  public void setHeader(Header header) {
    req.setHeader(header);
  }

  @Override
  public void setHeader(String name, String value) {
    req.setHeader(name, value);
  }

  @Override
  public void setHeaders(Header[] headers) {
    req.setHeaders(headers);
  }

  @Override
  public void removeHeader(Header header) {
    req.removeHeader(header);
  }

  @Override
  public void removeHeaders(String name) {
    req.removeHeaders(name);
  }

  @Override
  public HeaderIterator headerIterator() {
    removeHeaders("Via");
    return req.headerIterator();
  }

  @Override
  public HeaderIterator headerIterator(String name) {
    removeHeaders("Via");
    return req.headerIterator(name);
  }

  @Override
  public HttpParams getParams() {
    removeHeaders("Via");
    return req.getParams();
  }

  @Override
  public void setParams(HttpParams params) {
    req.setParams(params);
  }

  @Override
  public RequestLine getRequestLine() {
    return req.getRequestLine();
  }

}
