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

import org.apache.http.HttpException;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;

public class CustomCachingExec extends CachingExec {
  public CustomCachingExec(
      final ClientExecChain backend,
      final HttpCache cache,
      final CacheConfig config) {
    super(backend, cache, config);
  }

  public CustomCachingExec(
      final ClientExecChain backend,
      final HttpCache cache,
      final CacheConfig config,
      final AsynchronousValidator asynchRevalidator) {
    super(backend, cache, config, asynchRevalidator);
  }

  public CustomCachingExec(
      final ClientExecChain backend,
      final ResourceFactory resourceFactory,
      final HttpCacheStorage storage,
      final CacheConfig config) {
    super(backend, resourceFactory, storage, config);
  }

  public CustomCachingExec(final ClientExecChain backend) {
    super(backend);
  }

  @Override
  CloseableHttpResponse callBackend(
      final HttpRoute route,
      final HttpRequestWrapper request,
      final HttpClientContext context,
      final HttpExecutionAware execAware) throws IOException, HttpException {
    request.removeHeaders("Via");
    return super.callBackend(route, request, context, execAware);
  }
}
