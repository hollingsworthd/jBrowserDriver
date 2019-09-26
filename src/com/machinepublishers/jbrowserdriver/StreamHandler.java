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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

class StreamHandler implements URLStreamHandlerFactory {
  StreamHandler() {}

  private static class HttpHandler extends URLStreamHandler {
    @Override
    protected int getDefaultPort() {
      return 80;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      return new StreamConnection(url);
    }
  }

  private static class HttpsHandler extends URLStreamHandler {
    @Override
    protected int getDefaultPort() {
      return 443;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      return new StreamConnection(url);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {
    if ("http".equals(protocol)) {
      return new HttpHandler();
    }
    if ("https".equals(protocol)) {
      return new HttpsHandler();
    }
    if ("about".equals(protocol)) {
      return new com.sun.webkit.network.about.Handler();
    }
    if ("data".equals(protocol)) {
      return new com.sun.webkit.network.data.Handler();
    }
    return null;
  }

}
