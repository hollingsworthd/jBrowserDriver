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
package com.machinepublishers.jbrowserdriver.diagnostics;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.openqa.selenium.logging.LogEntry;

public class WireLog implements Log, Serializable {
  private static final Pattern request = Pattern.compile("^http-outgoing-[0-9]+\\s>>\\s\"(.+)\"$");
  private static final Pattern response = Pattern.compile(
      "^http-outgoing-[0-9]+\\s<<\\s\"((?:HTTP/.+)|(?:[^:]+:\\s.+))\"$");
  private static final int MAX_LEN = 500;

  public WireLog(String s) {}

  @Override
  public void debug(Object objMessage) {
    if (objMessage != null) {
      String message = objMessage.toString();
      if (message != null && message.length() < MAX_LEN) {
        Matcher matcher = request.matcher(message);
        if (matcher.matches()) {
          message = matcher.group(1).replace("[\\r]", "").replace("[\\n]", "");
          if (!message.isEmpty()) {
            System.out.println(new LogEntry(Level.FINEST, System.currentTimeMillis(),
                "----->> " + message));
          }
        } else {
          matcher = response.matcher(message);
          if (matcher.matches()) {
            message = matcher.group(1).replace("[\\r]", "").replace("[\\n]", "");
            if (!message.isEmpty()) {
              System.out.println(new LogEntry(Level.FINEST, System.currentTimeMillis(),
                  "<<----- " + message));
            }
          }
        }
      }
    }
  }

  @Override
  public void debug(Object message, Throwable t) {}

  @Override
  public void error(Object message) {}

  @Override
  public void error(Object message, Throwable t) {}

  @Override
  public void fatal(Object message) {}

  @Override
  public void fatal(Object message, Throwable t) {}

  @Override
  public void info(Object message) {}

  @Override
  public void info(Object message, Throwable t) {}

  @Override
  public void trace(Object message) {}

  @Override
  public void trace(Object message, Throwable t) {}

  @Override
  public void warn(Object message) {}

  @Override
  public void warn(Object message, Throwable t) {}

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public boolean isErrorEnabled() {
    return false;
  }

  @Override
  public boolean isFatalEnabled() {
    return false;
  }

  @Override
  public boolean isInfoEnabled() {
    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public boolean isWarnEnabled() {
    return false;
  }
}
