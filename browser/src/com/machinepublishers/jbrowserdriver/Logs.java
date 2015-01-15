/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;

import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

public class Logs implements org.openqa.selenium.logging.Logs {

  private static final LinkedList<LogEntry> entries = new LinkedList<LogEntry>();
  private static final int MAX_LOGS = 50;
  private static final Logs instance = new Logs();

  private Logs() {

  }

  public static Logs instance() {
    return instance;
  }

  public static void warn(String message) {
    synchronized (entries) {
      entries.add(new LogEntry(Level.WARNING, System.currentTimeMillis(), message));
      if (entries.size() > MAX_LOGS) {
        entries.removeFirst();
      }
    }
  }

  public static void exception(Throwable t) {
    synchronized (entries) {
      StringWriter writer = null;
      try {
        writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        entries.add(new LogEntry(Level.WARNING, System.currentTimeMillis(), writer.toString()));
        if (entries.size() > MAX_LOGS) {
          entries.removeFirst();
        }
      } finally {
        try {
          writer.close();
        } catch (Throwable t2) {}
      }
    }
  }

  @Override
  public LogEntries get(String s) {
    synchronized (entries) {
      LogEntries logEntries = new LogEntries(entries);
      entries.clear();
      return logEntries;
    }
  }

  @Override
  public Set<String> getAvailableLogTypes() {
    return new HashSet<String>(Arrays.asList(new String[] { "all" }));
  }
}
