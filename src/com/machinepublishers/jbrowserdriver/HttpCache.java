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
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.client.cache.HttpCacheUpdateException;

class HttpCache implements HttpCacheStorage {
  private final File cacheDir;

  HttpCache(File cacheDir) {
    this.cacheDir = cacheDir;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateEntry(String key, HttpCacheUpdateCallback callback) throws IOException, HttpCacheUpdateException {
    HttpCacheEntry entry = callback.update(getEntry(key));
    removeEntry(key);
    putEntry(key, entry);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEntry(String key) throws IOException {
    File file = new File(cacheDir, DigestUtils.sha1Hex(key));
    if (file.exists()) {
      try (Lock lock = new Lock(file, false)) {
        file.delete();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putEntry(String key, HttpCacheEntry entry) throws IOException {
    File file = new File(cacheDir, DigestUtils.sha1Hex(key));
    if (!file.exists()) {
      try (Lock lock = new Lock(file, false)) {
        FileOutputStream fileOut = new FileOutputStream(file);
        BufferedOutputStream bufferOut = new BufferedOutputStream(fileOut);
        try (ObjectOutputStream objectOut = new ObjectOutputStream(bufferOut)) {
          objectOut.writeObject(entry);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpCacheEntry getEntry(String key) throws IOException {
    File file = new File(cacheDir, DigestUtils.sha1Hex(key));
    if (file.exists()) {
      try (Lock lock = new Lock(file, true)) {
        FileInputStream fileIn = new FileInputStream(file);
        BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
        try (ObjectInputStream objectIn = new ObjectInputStream(bufferIn)) {
          return (HttpCacheEntry) objectIn.readObject();
        } catch (Throwable t) {
          LogsServer.instance().exception(t);
        }
      }
    }
    return null;
  }

  private static class Lock implements Closeable {
    private FileLock fileLock;
    private RandomAccessFile randAccessFile;

    Lock(File file, boolean shared) {
      try {
        randAccessFile = new RandomAccessFile(file, shared ? "r" : "rw");
        FileChannel channel = randAccessFile.getChannel();
        while (true) {
          try {
            fileLock = channel.lock(0, Long.MAX_VALUE, shared);
            break;
          } catch (Throwable t) {
            try {
              Thread.sleep(50);
            } catch (InterruptedException e) {}
          }
        }
      } catch (Throwable t) {
        LogsServer.instance().exception(t);
        close();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
      if (fileLock != null) {
        try {
          fileLock.release();
        } catch (Throwable t) {
          LogsServer.instance().exception(t);
        }
      }
      if (randAccessFile != null) {
        try {
          randAccessFile.close();
        } catch (Throwable t) {
          LogsServer.instance().exception(t);
        }
      }
    }
  }
}
