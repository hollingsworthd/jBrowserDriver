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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;

class Entries implements Serializable {

  private final List<Entry> entries = new ArrayList<Entry>();

  Entries(Iterable<Entry> iterable) {
    Iterator<Entry> iter = iterable.iterator();
    while (iter.hasNext()) {
      entries.add(iter.next());
    }
  }

  LogEntries toLogEntries() {
    List<LogEntry> list = new ArrayList<LogEntry>();
    for (Entry entry : entries) {
      list.add(entry.toLogEntry());
    }
    return new LogEntries(list);
  }
}
