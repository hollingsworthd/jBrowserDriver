/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
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

  @Override
  public LogEntries get(String s) {
    try {
      return remote.getRemote(s).toLogEntries();
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

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
