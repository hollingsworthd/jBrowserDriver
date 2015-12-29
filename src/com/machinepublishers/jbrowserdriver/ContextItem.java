/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
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
  final AtomicReference<NavigationServer> navigation = new AtomicReference<NavigationServer>();
  //TODO final AtomicReference<Alert> alert = new AtomicReference<Alert>();
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
        navigation.set(new NavigationServer(
            new AtomicReference<JBrowserDriverServer>(driver), view, context.statusCode));
        context.options.set(new OptionsServer(
            window, SettingsManager.settings().cookieStore(), context.timeouts));
      } catch (RemoteException e) {
        Logs.instance().exception(e);
      }
      Util.exec(Pause.SHORT, context.statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          httpListener.set(new HttpListener(
              context.statusCode, context.timeouts.get().getPageLoadTimeoutObjMS()));
          Accessor.getPageFor(view.get().getEngine()).addLoadListenerClient(httpListener.get());
          engine.get().setCreatePopupHandler(new PopupHandler(driver, context));
          //TODO engine.get().call("setConfirmHandler",
          //TODO JavaFx.getNew(DynamicConfirmHandler.class, context.settingsId.get(), driver, context));
          //TODO engine.get().call("setPromptHandler",
          //TODO JavaFx.getNew(DynamicPromptHandler.class, context.settingsId.get(), driver, context));
          return null;
        }
      });
    }
  }
}
