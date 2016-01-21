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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.client.cache.HttpCacheUpdateException;

class HttpCache implements HttpCacheStorage {
  private final File cacheDir;

  HttpCache() {
    File cacheDirTmp = null;
    try {
      cacheDirTmp = Files.createTempDirectory("jbdwebcache").toFile();
      cacheDirTmp.deleteOnExit();
    } catch (Throwable t) {
      LogsServer.instance().exception(t);
    }
    cacheDir = cacheDirTmp;
  }

  @Override
  public void updateEntry(String key, HttpCacheUpdateCallback callback) throws IOException, HttpCacheUpdateException {
    HttpCacheEntry entry = callback.update(getEntry(key));
    removeEntry(key);
    putEntry(key, entry);
  }

  @Override
  public void removeEntry(String key) throws IOException {
    new File(cacheDir, DigestUtils.sha1Hex(key)).delete();
  }

  @Override
  public void putEntry(String key, HttpCacheEntry entry) throws IOException {
    File file = new File(cacheDir, DigestUtils.sha1Hex(key));
    file.deleteOnExit();
    FileOutputStream fileOut = new FileOutputStream(file);
    BufferedOutputStream bufferOut = new BufferedOutputStream(fileOut);
    try (ObjectOutputStream objectOut = new ObjectOutputStream(bufferOut)) {
      objectOut.writeObject(entry);
    }
  }

  @Override
  public HttpCacheEntry getEntry(String key) throws IOException {
    File file = new File(cacheDir, DigestUtils.sha1Hex(key));
    if (file.exists()) {
      FileInputStream fileIn = new FileInputStream(file);
      BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
      try (ObjectInputStream objectIn = new ObjectInputStream(bufferIn)) {
        return (HttpCacheEntry) objectIn.readObject();
      } catch (Throwable t) {
        LogsServer.instance().exception(t);
      }
    }
    return null;
  }
}
