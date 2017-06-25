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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class LogHandler extends Handler {
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  static {
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @Override
  public void publish(LogRecord record) {
    String message = new StringBuilder()
        .append("[")
        .append(dateFormat.format(new Date(record.getMillis())))
        .append("]")
        .append(record.getMessage()).toString();
    if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
      System.err.println(message);
    } else {
      System.out.println(message);
    }
  }

  @Override
  public void flush() {}

  @Override
  public void close() throws SecurityException {}
}
