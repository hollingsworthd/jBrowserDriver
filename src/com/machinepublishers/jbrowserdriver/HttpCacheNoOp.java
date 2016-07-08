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

import java.io.IOException;

import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.client.cache.HttpCacheUpdateException;

class HttpCacheNoOp implements HttpCacheStorage {

  HttpCacheNoOp() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateEntry(String key, HttpCacheUpdateCallback callback) throws IOException, HttpCacheUpdateException {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEntry(String key) throws IOException {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void putEntry(String key, HttpCacheEntry entry) throws IOException {}

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpCacheEntry getEntry(String key) throws IOException {
    return null;
  }
}
