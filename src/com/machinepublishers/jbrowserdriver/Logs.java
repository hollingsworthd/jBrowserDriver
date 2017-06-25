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

import java.util.Set;

import org.openqa.selenium.logging.LogEntries;

class Logs implements org.openqa.selenium.logging.Logs {
  private final LogsRemote remote;
  private final SocketLock lock;

  Logs(LogsRemote remote, SocketLock lock) {
    this.remote = remote;
    this.lock = lock;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LogEntries get(String type) {
    try {
      synchronized (lock.validated()) {
        return remote.getRemote(type).toLogEntries();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getAvailableLogTypes() {
    try {
      synchronized (lock.validated()) {
        return remote.getAvailableLogTypes();
      }
    } catch (Throwable t) {
      Util.handleException(t);
      return null;
    }
  }
}
