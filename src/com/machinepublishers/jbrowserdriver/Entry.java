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

import java.io.Serializable;
import java.util.logging.Level;

import org.openqa.selenium.logging.LogEntry;

class Entry implements Serializable {

  private final Level level;
  private final long timestamp;
  private final String message;

  Entry(Level level, long timestamp, String message) {
    this.level = level;
    this.timestamp = timestamp;
    this.message = message;
  }

  LogEntry toLogEntry() {
    return new LogEntry(level, timestamp, message);
  }

  @Override
  public String toString() {
    return toLogEntry().toString();
  }
}
