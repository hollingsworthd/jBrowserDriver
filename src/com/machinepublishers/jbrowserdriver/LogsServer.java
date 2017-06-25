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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.openqa.selenium.logging.LogEntries;

@SuppressWarnings("deprecation") //WireLog is not actually deprecated--it's for internal use only
class LogsServer extends RemoteObject implements LogsRemote, org.openqa.selenium.logging.Logs {
  private static class WireAppender implements Appendable {
    @Override
    public Appendable append(CharSequence csq) throws IOException {
      LogsServer.instance().wire(csq.toString());
      return null;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
      return null;
    }

    @Override
    public Appendable append(char c) throws IOException {
      return null;
    }
  }

  static {
    WireLog.setAppender(new WireAppender());
  }

  private final LinkedList<Entry> wire = new LinkedList<Entry>();
  private final LinkedList<Entry> javascript = new LinkedList<Entry>();
  private final LinkedList<Entry> trace = new LinkedList<Entry>();
  private final LinkedList<Entry> warn = new LinkedList<Entry>();
  private static final LogsServer instance;
  static {
    LogsServer instanceTmp = null;
    try {
      instanceTmp = new LogsServer();
    } catch (RemoteException e) {
      Util.handleException(e);
    }
    instance = instanceTmp;
  }

  static void updateSettings() {
    Settings settings = SettingsManager.settings();
    if (settings != null && settings.logWire()) {
      System.setProperty("org.apache.commons.logging.Log", "com.machinepublishers.jbrowserdriver.WireLog");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");
    } else {
      System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "OFF");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "OFF");
    }
  }

  static LogsServer instance() {
    return instance;
  }

  private LogsServer() throws RemoteException {}

  public void clear(String type) {
    handleEntries(false, type);
  }

  private static void handleMessage(String message, LinkedList<Entry> entries, Level level,
      String type, Settings settings) {
    if (settings != null && settings.logsMax() > 0) {
      final Entry entry = new Entry(level, System.currentTimeMillis(), message);
      synchronized (entries) {
        entries.add(entry);
        if (entries.size() > settings.logsMax()) {
          entries.removeFirst();
        }
      }
    }
    if (settings == null || level.intValue() >= settings.loggerLevel()) {
      System.err.println(">" + level.getName() + "/" + type + "/" + message);
    }
  }

  private synchronized Entries handleEntries(boolean aggregate, String type) {
    List<Entry> combinedLogs = new ArrayList<Entry>();
    boolean all = type == null || "all".equals(type);
    if (all || "wire".equals(type)) {
      synchronized (wire) {
        if (aggregate) {
          combinedLogs.addAll(wire);
        }
        wire.clear();
      }
    }
    if (all || "javascript".equals(type)) {
      synchronized (javascript) {
        if (aggregate) {
          combinedLogs.addAll(javascript);
        }
        javascript.clear();
      }
    }
    if (all || "trace".equals(type)) {
      synchronized (trace) {
        if (aggregate) {
          combinedLogs.addAll(trace);
        }
        trace.clear();
      }
    }
    if (all || "warnings".equals(type)) {
      synchronized (warn) {
        if (aggregate) {
          combinedLogs.addAll(warn);
        }
        warn.clear();
      }
    }
    Entries logEntries = new Entries(combinedLogs);
    return logEntries;
  }

  public void wire(String message) {
    Settings settings = SettingsManager.settings();
    if (settings != null && settings.logWire()) {
      handleMessage(message, wire, Level.FINEST, "wire", settings);
    }
  }

  public void javascript(String message) {
    Settings settings = SettingsManager.settings();
    if (settings != null && settings.logJavascript()) {
      handleMessage(message, javascript, Level.FINER, "javascript", settings);
    }
  }

  public void trace(String message) {
    Settings settings = SettingsManager.settings();
    if (settings != null && settings.logTrace()) {
      handleMessage(message, trace, Level.INFO, "trace", settings);
    }
  }

  public void warn(String message) {
    Settings settings = SettingsManager.settings();
    if (settings == null || settings.logWarnings()) {
      handleMessage(message, warn, Level.WARNING, "warnings", settings);
    }
  }

  public void exception(Throwable throwable) {
    String message = null;
    StringWriter writer = null;
    try {
      writer = new StringWriter();
      throwable.printStackTrace(new PrintWriter(writer));
      message = writer.toString();
    } catch (Throwable t) {
      message = "While logging a message, an error occurred: " + t.getMessage();
    } finally {
      Util.close(writer);
    }
    warn(message);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Entries getRemote(String type) {
    return handleEntries(true, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LogEntries get(String type) {
    return getRemote(type).toLogEntries();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getAvailableLogTypes() {
    return new HashSet<String>(Arrays.asList(new String[] { "all", "wire", "javascript", "trace", "warnings" }));
  }
}
