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

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.params.HttpParams;

public class CustomResponse implements CloseableHttpResponse {

  private final CloseableHttpResponse resp;

  public CustomResponse(CloseableHttpResponse resp) {
    this.resp = resp;
  }

  private void adjustHeaders() {
    resp.removeHeaders("Via");
    if (ArrayUtils.isEmpty(resp.getHeaders("Content-Type"))) {
      resp.addHeader("Content-Type", "text/html");
    }
  }

  @Override
  public StatusLine getStatusLine() {
    return resp.getStatusLine();
  }

  @Override
  public void setStatusLine(StatusLine statusline) {
    resp.setStatusLine(statusline);
  }

  @Override
  public void setStatusLine(ProtocolVersion ver, int code) {
    resp.setStatusLine(ver, code);
  }

  @Override
  public void setStatusLine(ProtocolVersion ver, int code, String reason) {
    resp.setStatusLine(ver, code, reason);
  }

  @Override
  public void setStatusCode(int code) throws IllegalStateException {
    resp.setStatusCode(code);
  }

  @Override
  public void setReasonPhrase(String reason) throws IllegalStateException {
    resp.setReasonPhrase(reason);
  }

  @Override
  public HttpEntity getEntity() {
    adjustHeaders();
    return resp.getEntity();
  }

  @Override
  public void setEntity(HttpEntity entity) {
    resp.setEntity(entity);
  }

  @Override
  public Locale getLocale() {
    return resp.getLocale();
  }

  @Override
  public void setLocale(Locale loc) {
    resp.setLocale(loc);
  }

  @Override
  public ProtocolVersion getProtocolVersion() {
    return resp.getProtocolVersion();
  }

  @Override
  public boolean containsHeader(String name) {
    adjustHeaders();
    return resp.containsHeader(name);
  }

  @Override
  public Header[] getHeaders(String name) {
    adjustHeaders();
    return resp.getHeaders(name);
  }

  @Override
  public Header getFirstHeader(String name) {
    adjustHeaders();
    return resp.getFirstHeader(name);
  }

  @Override
  public Header getLastHeader(String name) {
    adjustHeaders();
    return resp.getLastHeader(name);
  }

  @Override
  public Header[] getAllHeaders() {
    adjustHeaders();
    return resp.getAllHeaders();
  }

  @Override
  public void addHeader(Header header) {
    resp.addHeader(header);
  }

  @Override
  public void addHeader(String name, String value) {
    resp.addHeader(name, value);
  }

  @Override
  public void setHeader(Header header) {
    resp.setHeader(header);
  }

  @Override
  public void setHeader(String name, String value) {
    resp.setHeader(name, value);
  }

  @Override
  public void setHeaders(Header[] headers) {
    resp.setHeaders(headers);
  }

  @Override
  public void removeHeader(Header header) {
    resp.removeHeader(header);
  }

  @Override
  public void removeHeaders(String name) {
    resp.removeHeaders(name);
  }

  @Override
  public HeaderIterator headerIterator() {
    adjustHeaders();
    return resp.headerIterator();
  }

  @Override
  public HeaderIterator headerIterator(String name) {
    adjustHeaders();
    return resp.headerIterator(name);
  }

  @Override
  public HttpParams getParams() {
    adjustHeaders();
    return resp.getParams();
  }

  @Override
  public void setParams(HttpParams params) {
    resp.setParams(params);
  }

  @Override
  public void close() throws IOException {
    resp.close();
  }
}
