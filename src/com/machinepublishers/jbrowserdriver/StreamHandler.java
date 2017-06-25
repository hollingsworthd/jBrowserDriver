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

  private static class HttpHandler extends sun.net.www.protocol.http.Handler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      return new StreamConnection(url);
    }

    private URLConnection defaultConnection(URL url) throws IOException {
      return super.openConnection(url);
    }
  }

  private static class HttpsHandler extends sun.net.www.protocol.https.Handler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      return new StreamConnection(url);
    }

    private URLConnection defaultConnection(URL url) throws IOException {
      return super.openConnection(url);
    }
  }

  static URLConnection defaultConnection(URL url) throws IOException {
    return "https".equals(url.getProtocol())
        ? new HttpsHandler().defaultConnection(url) : new HttpHandler().defaultConnection(url);
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
    if ("file".equals(protocol)) {
      return new sun.net.www.protocol.file.Handler();
    }
    if ("ftp".equals(protocol)) {
      return new sun.net.www.protocol.ftp.Handler();
    }
    if ("jar".equals(protocol)) {
      return new sun.net.www.protocol.jar.Handler();
    }
    if ("mailto".equals(protocol)) {
      return new sun.net.www.protocol.mailto.Handler();
    }
    return null;
  }

}
