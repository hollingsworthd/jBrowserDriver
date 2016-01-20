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
package com.machinepublishers.jbrowserdriver;

import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.sun.javafx.webkit.Accessor;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

class ContextItem {
  private static final AtomicLong currentItemId = new AtomicLong();

  final AtomicReference<WindowServer> window = new AtomicReference<WindowServer>();
  final AtomicReference<Stage> stage = new AtomicReference<Stage>();
  final AtomicReference<WebView> view = new AtomicReference<WebView>();
  final AtomicReference<WebEngine> engine = new AtomicReference<WebEngine>();
  final AtomicReference<HttpListener> httpListener = new AtomicReference<HttpListener>();
  final AtomicBoolean initialized = new AtomicBoolean();
  final AtomicReference<String> itemId = new AtomicReference<String>();

  ContextItem() {
    itemId.set(Long.toString(currentItemId.getAndIncrement()));
  }

  void init(final JBrowserDriverServer driver, final Context context) {
    if (initialized.compareAndSet(false, true)) {
      SettingsManager.register(stage, view);
      engine.set(view.get().getEngine());
      try {
        window.set(new WindowServer(stage, context.statusCode));
        context.dialog.get().listen(this);
      } catch (RemoteException e) {
        LogsServer.instance().exception(e);
      }
      Util.exec(Pause.SHORT, context.statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          httpListener.set(new HttpListener(
              context.statusCode, context.timeouts.get().getPageLoadTimeoutObjMS()));
          Accessor.getPageFor(view.get().getEngine()).addLoadListenerClient(httpListener.get());
          engine.get().setCreatePopupHandler(new PopupHandler(driver, context));
          return null;
        }
      });
    }
  }
}
