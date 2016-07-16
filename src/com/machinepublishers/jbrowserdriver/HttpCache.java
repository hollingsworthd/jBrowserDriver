/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC and the jBrowserDriver contributors
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashSet;
import java.util.Set;

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
      try {
        file.createNewFile();
      } catch (Throwable t) {}
      try (Lock lock = new Lock(file, false)) {
        BufferedOutputStream bufferOut = new BufferedOutputStream(lock.streamOut);
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
        BufferedInputStream bufferIn = new BufferedInputStream(lock.streamIn);
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
    private static final Set<String> locks = new HashSet<String>();
    private String lockName;
    private FileLock fileLock;
    private FileInputStream streamIn;
    private FileOutputStream streamOut;
    private FileChannel channel;

    Lock(File file, boolean read) {
      this.lockName = file.getAbsolutePath();
      synchronized (locks) {
        while (true) {
          try {
            if (!locks.contains(lockName)) {
              locks.add(lockName);
              break;
            } else {
              locks.wait();
            }
          } catch (Throwable t) {}
        }
      }
      try {
        if (read) {
          streamIn = new FileInputStream(file);
          channel = streamIn.getChannel();
        } else {
          streamOut = new FileOutputStream(file);
          channel = streamOut.getChannel();
        }
        while (true) {
          try {
            fileLock = channel.lock(0, Long.MAX_VALUE, read);
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
      if (fileLock != null && channel != null && channel.isOpen()) {
        try {
          fileLock.release();
        } catch (Throwable t) {
          LogsServer.instance().exception(t);
        }
      }
      if (streamIn != null) {
        try {
          streamIn.close();
        } catch (Throwable t) {
          LogsServer.instance().exception(t);
        }
      }
      if (streamOut != null) {
        try {
          streamOut.close();
        } catch (Throwable t) {
          LogsServer.instance().exception(t);
        }
      }
      synchronized (locks) {
        locks.remove(lockName);
        locks.notifyAll();
      }
    }
  }
}
