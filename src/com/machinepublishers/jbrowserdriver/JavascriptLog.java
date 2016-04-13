package com.machinepublishers.jbrowserdriver;

import java.util.concurrent.atomic.AtomicInteger;

import com.machinepublishers.jbrowserdriver.AppThread.Pause;
import com.machinepublishers.jbrowserdriver.AppThread.Sync;
import com.sun.webkit.WebPage;

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
import netscape.javascript.JSObject;

class JavascriptLog {

  private static final Bridge bridge = new Bridge();
  private static final String consoleScript;
  static {
    consoleScript = new StringBuilder()
        .append("console.error = ").append(jsConsoleFunction("error"))
        .append("console.warn = ").append(jsConsoleFunction("warn"))
        .append("console.log = ").append(jsConsoleFunction("log"))
        .append("console.info = ").append(jsConsoleFunction("info"))
        .append("console.assert = ").append(jsConsoleFunction("jsAssert")).toString();
  }

  static void attach(WebPage page, long frameId) {
    AppThread.exec(Pause.NONE, new AtomicInteger(-1), new Sync<Object>() {
      @Override
      public Object perform() {
        JSObject window = (JSObject) page.executeScript(frameId, "(function(){return window;})();");
        if (window != null) {
          window.setMember("javascriptLog", bridge);
          page.executeScript(frameId, consoleScript);
        }
        return null;
      }
    });
  }

  private static final String jsConsoleFunction(String functionName) {
    return new StringBuilder()
        .append("function(){try{javascriptLog.").append(functionName).append("(JSON.stringify(arguments));}")
        .append("catch(ex){javascriptLog.").append(functionName).append("('(WebDriver message could not be stringified.)');}};")
        .toString();
  }

  private JavascriptLog() {

  }

  public static class Bridge {
    private Bridge() {}

    public void error(String message) {
      LogsServer.instance().javascript("console.error: " + message);
    }

    public void warn(String message) {
      LogsServer.instance().javascript("console.warn: " + message);
    }

    public void log(String message) {
      LogsServer.instance().javascript("console.log: " + message);
    }

    public void info(String message) {
      LogsServer.instance().javascript("console.info: " + message);
    }

    public void jsAssert(String message) {
      LogsServer.instance().javascript("console.assert: " + message);
    }
  }
}