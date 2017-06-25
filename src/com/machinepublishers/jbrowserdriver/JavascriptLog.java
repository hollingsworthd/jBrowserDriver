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

import com.machinepublishers.jbrowserdriver.AppThread.Sync;
import com.sun.webkit.WebPage;

import netscape.javascript.JSObject;

class JavascriptLog {

  private static final String bridgeName = Util.randomPropertyName();
  private static final Bridge bridge = new Bridge();
  private static final String consoleScript;
  static {
    consoleScript = new StringBuilder()
        .append(consoleFunction("error"))
        .append(consoleFunction("warn"))
        .append(consoleFunction("log"))
        .append(consoleFunction("info"))
        .append(consoleFunction("assert"))
        .append("window.addEventListener('error',function(){")
        .append("try{")
        .append(bridgeName).append(".log(JSON.stringify(")
        .append("{window_onerror:arguments[0].message, filename:arguments[0].filename, lineno:arguments[0].lineno}")
        .append("));}catch(ex){")
        .append(bridgeName).append(".log(JSON.stringify({")
        .append("window_onerror:'WebDriver message could not be stringified.'}));}")
        .append("return false;});").toString();
  }

  static void attach(WebPage page, long frameId) {
    AppThread.exec(
        new Sync<Object>() {
          @Override
          public Object perform() {
            JSObject window = (JSObject) page.executeScript(frameId, "(function(){return window;})();");
            if (window != null) {
              window.setMember(bridgeName, bridge);
              page.executeScript(frameId, consoleScript);
            }
            return null;
          }
        });
  }

  private static final String consoleFunction(String jsName) {
    return new StringBuilder()
        .append("try{")
        .append("Object.defineProperty(console,'")
        .append(jsName).append("',{get: (function(){return function(){try{")
        .append("var arr = [];")
        .append("for(var i in arguments){")
        .append("arr.push(arguments[i]);")
        .append("}")
        .append(bridgeName).append(".log(JSON.stringify({console_").append(jsName).append(":arr}));")
        .append("}catch(ex){")
        .append(bridgeName).append(".").append("log(JSON.stringify({")
        .append("console_")
        .append(jsName).append(":'WebDriver message could not be stringified.'}));")
        .append("}};})});}catch(ex){}")
        .toString();
  }

  private JavascriptLog() {

  }

  public static class Bridge {
    private Bridge() {}

    public void log(String message) {
      LogsServer.instance().javascript(message);
    }
  }
}