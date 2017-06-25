/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
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
import java.io.FileNotFoundException;
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
    putEntry(key, entry);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeEntry(String key) throws IOException {
    try (Lock lock = new Lock(new File(cacheDir, DigestUtils.sha1Hex(key)), false, false)) {
      lock.file.delete();
    } catch (FileNotFoundException e) {
      //ignore
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putEntry(String key, HttpCacheEntry entry) throws IOException {
    try (Lock lock = new Lock(new File(cacheDir, DigestUtils.sha1Hex(key)), false, true)) {
      BufferedOutputStream bufferOut = new BufferedOutputStream(lock.streamOut);
      try (ObjectOutputStream objectOut = new ObjectOutputStream(bufferOut)) {
        objectOut.writeObject(entry);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpCacheEntry getEntry(String key) throws IOException {
    try (Lock lock = new Lock(new File(cacheDir, DigestUtils.sha1Hex(key)), true, false)) {
      BufferedInputStream bufferIn = new BufferedInputStream(lock.streamIn);
      try (ObjectInputStream objectIn = new ObjectInputStream(bufferIn)) {
        return (HttpCacheEntry) objectIn.readObject();
      } catch (Throwable t) {
        LogsServer.instance().exception(t);
      }
    } catch (FileNotFoundException e) {
      return null;
    }
    return null;
  }

  private static class Lock implements Closeable {
    private static final int MAX_RETRIES = 50;
    private static final int RETRY_SLEEP = 100;
    private static final Set<String> locks = new HashSet<String>();
    private String lockName;
    private File file;
    private FileLock fileLock;
    private FileInputStream streamIn;
    private FileOutputStream streamOut;
    private FileChannel channel;

    Lock(File file, boolean read, boolean create) throws IOException {
      this.file = file;
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
      for (int i = 0; i < MAX_RETRIES; i++) {
        if (i > 0) {
          try {
            Thread.sleep(RETRY_SLEEP);
          } catch (InterruptedException e2) {}
        }
        try {
          if (create) {
            file.createNewFile();
          }
          if (read) {
            streamIn = new FileInputStream(file);
            channel = streamIn.getChannel();
          } else {
            streamOut = new FileOutputStream(file);
            channel = streamOut.getChannel();
          }
          fileLock = channel.lock(0, Long.MAX_VALUE, read);
          break;
        } catch (Throwable t) {
          if (!create && t instanceof FileNotFoundException) {
            close();
            throw t;
          }
          if (i + 1 == MAX_RETRIES) {
            LogsServer.instance().exception(t);
            close();
            if (t instanceof IOException) {
              throw t;
            }
            throw new IOException(t);
          }
          closeStreams();
        }
      }
    }

    private void closeStreams() {
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
      closeStreams();
      synchronized (locks) {
        locks.remove(lockName);
        locks.notifyAll();
      }
    }
  }
}
