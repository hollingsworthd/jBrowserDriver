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

/*
 * Note: this is a modified and renamed version of
 * org.apache.http.impl.client.cache.CachingHttpClientBuilder
 * which is Copyright 2010-2015 The Apache Software Foundation [http://www.apache.org/]
 * and licensed under the Apache License, Version 2.0 (the "License");
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.apache.http.client.cache.HttpCacheInvalidator;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.execchain.ClientExecChain;

/**
 * Builder for {@link org.apache.http.impl.client.CloseableHttpClient}
 * instances capable of client-side caching.
 *
 * @since 4.3
 */
public class CustomCachingHttpClientBuilder extends HttpClientBuilder {

  private ResourceFactory resourceFactory;
  private HttpCacheStorage storage;
  private File cacheDir;
  private CacheConfig cacheConfig;
  private SchedulingStrategy schedulingStrategy;
  private HttpCacheInvalidator httpCacheInvalidator;
  private boolean deleteCache;

  public static CustomCachingHttpClientBuilder create() {
    return new CustomCachingHttpClientBuilder();
  }

  protected CustomCachingHttpClientBuilder() {
    super();
    this.deleteCache = true;
  }

  public final CustomCachingHttpClientBuilder setResourceFactory(
      final ResourceFactory resourceFactory) {
    this.resourceFactory = resourceFactory;
    return this;
  }

  public final CustomCachingHttpClientBuilder setHttpCacheStorage(
      final HttpCacheStorage storage) {
    this.storage = storage;
    return this;
  }

  public final CustomCachingHttpClientBuilder setCacheDir(
      final File cacheDir) {
    this.cacheDir = cacheDir;
    return this;
  }

  public final CustomCachingHttpClientBuilder setCacheConfig(
      final CacheConfig cacheConfig) {
    this.cacheConfig = cacheConfig;
    return this;
  }

  public final CustomCachingHttpClientBuilder setSchedulingStrategy(
      final SchedulingStrategy schedulingStrategy) {
    this.schedulingStrategy = schedulingStrategy;
    return this;
  }

  public final CustomCachingHttpClientBuilder setHttpCacheInvalidator(
      final HttpCacheInvalidator cacheInvalidator) {
    this.httpCacheInvalidator = cacheInvalidator;
    return this;
  }

  public CustomCachingHttpClientBuilder setDeleteCache(final boolean deleteCache) {
    this.deleteCache = deleteCache;
    return this;
  }

  @Override
  protected ClientExecChain decorateMainExec(final ClientExecChain mainExec) {
    final CacheConfig config = this.cacheConfig != null ? this.cacheConfig : CacheConfig.DEFAULT;
    // We copy the instance fields to avoid changing them, and rename to avoid accidental use of the wrong version
    ResourceFactory resourceFactoryCopy = this.resourceFactory;
    if (resourceFactoryCopy == null) {
      if (this.cacheDir == null) {
        resourceFactoryCopy = new HeapResourceFactory();
      } else {
        resourceFactoryCopy = new FileResourceFactory(cacheDir);
      }
    }
    HttpCacheStorage storageCopy = this.storage;
    if (storageCopy == null) {
      if (this.cacheDir == null) {
        storageCopy = new BasicHttpCacheStorage(config);
      } else {
        final ManagedHttpCacheStorage managedStorage = new ManagedHttpCacheStorage(config);
        if (this.deleteCache) {
          addCloseable(new Closeable() {

            @Override
            public void close() throws IOException {
              managedStorage.shutdown();
            }

          });
        } else {
          addCloseable(managedStorage);
        }
        storageCopy = managedStorage;
      }
    }
    final AsynchronousValidator revalidator = createAsynchronousRevalidator(config);
    final CacheKeyGenerator uriExtractor = new CacheKeyGenerator();

    HttpCacheInvalidator cacheInvalidator = this.httpCacheInvalidator;
    if (cacheInvalidator == null) {
      cacheInvalidator = new CacheInvalidator(uriExtractor, storageCopy);
    }

    //MODIFIED to use CustomCachingExec
    return new CustomCachingExec(mainExec,
        new BasicHttpCache(
            resourceFactoryCopy,
            storageCopy, config,
            uriExtractor,
            cacheInvalidator),
        config, revalidator);
  }

  private AsynchronousValidator createAsynchronousRevalidator(final CacheConfig config) {
    if (config.getAsynchronousWorkersMax() > 0) {
      final SchedulingStrategy configuredSchedulingStrategy = createSchedulingStrategy(config);
      final AsynchronousValidator revalidator = new AsynchronousValidator(
          configuredSchedulingStrategy);
      addCloseable(revalidator);
      return revalidator;
    }
    return null;
  }

  private SchedulingStrategy createSchedulingStrategy(final CacheConfig config) {
    return schedulingStrategy != null ? schedulingStrategy : new ImmediateSchedulingStrategy(config);
  }

}
