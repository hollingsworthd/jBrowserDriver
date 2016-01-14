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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.logging.Level;

import org.openqa.selenium.logging.LogEntries;

class Logs implements org.openqa.selenium.logging.Logs {
  private final LogsRemote remote;

  Logs(LogsRemote remote) {
    this.remote = remote;
  }

  void clear() {
    try {
      remote.clear();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  void trace(String message) {
    try {
      remote.trace(message);
    } catch (Throwable t) {
      t.printStackTrace();
      System.out.println(new Entry(Level.FINEST, System.currentTimeMillis(), message));
    }
  }

  void warn(String message) {
    try {
      remote.warn(message);
    } catch (Throwable t) {
      t.printStackTrace();
      System.err.println(new Entry(Level.WARNING, System.currentTimeMillis(), message));
    }
  }

  void exception(Throwable throwable) {
    try {
      remote.exception(throwable);
    } catch (Throwable t) {
      t.printStackTrace();
      try (StringWriter writer = new StringWriter()) {
        throwable.printStackTrace(new PrintWriter(writer));
        System.err.println(new Entry(Level.WARNING, System.currentTimeMillis(), writer.toString()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LogEntries get(String s) {
    try {
      return remote.getRemote(s).toLogEntries();
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getAvailableLogTypes() {
    try {
      return remote.getAvailableLogTypes();
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }
}
