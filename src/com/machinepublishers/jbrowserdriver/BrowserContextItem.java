/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

class BrowserContextItem {
  private static final AtomicLong currentItemId = new AtomicLong();

  final AtomicReference<Window> window = new AtomicReference<Window>();
  final AtomicReference<Navigation> navigation = new AtomicReference<Navigation>();
  //TODO final AtomicReference<Alert> alert = new AtomicReference<Alert>();
  final AtomicReference<JavaFxObject> stage = new AtomicReference<JavaFxObject>();
  final AtomicReference<JavaFxObject> view = new AtomicReference<JavaFxObject>();
  final AtomicReference<JavaFxObject> engine = new AtomicReference<JavaFxObject>();
  final AtomicReference<JavaFxObject> httpListener = new AtomicReference<JavaFxObject>();
  final AtomicBoolean initialized = new AtomicBoolean();
  final AtomicReference<String> itemId = new AtomicReference<String>();

  BrowserContextItem() {
    itemId.set(Long.toString(currentItemId.getAndIncrement()));
  }

  void init(final JBrowserDriver driver, final BrowserContext context) {
    if (initialized.compareAndSet(false, true)) {
      SettingsManager.register(stage, view, context.settings);
      engine.set(view.get().call("getEngine"));
      window.set(new Window(stage, context.statusCode, context.settingsId.get()));
      navigation.set(new Navigation(
          new AtomicReference<JBrowserDriver>(driver), view, context.statusCode, context.settingsId.get()));
      context.options.set(new Options(
          window, context.logs, context.settings.get().cookieStore(), context.timeouts));
      Util.exec(Pause.SHORT, context.statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          httpListener.set(JavaFx.getNew("com.machinepublishers.jbrowserdriver.DynamicHttpListener", context.settingsId.get(),
              context.statusCode, context.timeouts.get().getPageLoadTimeoutObjMS(),
              context.settingsId.get()));
          JavaFx.getStatic("com.sun.javafx.webkit.Accessor", context.settingsId.get()).call("getPageFor", view.get().call("getEngine")).call("addLoadListenerClient", httpListener.get());
          engine.get().call("setCreatePopupHandler",
              JavaFx.getNew("com.machinepublishers.jbrowserdriver.DynamicPopupHandler", context.settingsId.get(), driver, context));
          //TODO engine.get().call("setConfirmHandler",
          //TODO JavaFx.getNew(DynamicConfirmHandler.class, context.settingsId.get(), driver, context));
          //TODO engine.get().call("setPromptHandler",
          //TODO JavaFx.getNew(DynamicPromptHandler.class, context.settingsId.get(), driver, context));
          return null;
        }
      }, context.settingsId.get());
    }
  }
}