/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * jBrowserDriver is made available under the terms of the GNU Affero General Public License version 3
 * with the following clarification and special exception:
 *
 *   Linking jBrowserDriver statically or dynamically with other modules is making a combined work
 *   based on jBrowserDriver. Thus, the terms and conditions of the GNU Affero General Public License
 *   version 3 cover the whole combination.
 *
 *   As a special exception, Machine Publishers, LLC gives you permission to link unmodified versions
 *   of jBrowserDriver with independent modules to produce an executable, regardless of the license
 *   terms of these independent modules, and to copy, distribute, and make available the resulting
 *   executable under terms of your choice, provided that you also meet, for each linked independent
 *   module, the terms and conditions of the license of that module. An independent module is a module
 *   which is not derived from or based on jBrowserDriver. If you modify jBrowserDriver, you may not
 *   extend this exception to your modified version of jBrowserDriver.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For general details about how to investigate and report license violations, please see:
 * <https://www.gnu.org/licenses/gpl-violation.html> and email the author: ops@machinepublishers.com
 */
package com.machinepublishers.jbrowserdriver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.sun.javafx.webkit.Accessor;

class BrowserContextItem {
  private static final AtomicLong currentItemId = new AtomicLong();

  final AtomicReference<Window> window = new AtomicReference<Window>();
  final AtomicReference<Navigation> navigation = new AtomicReference<Navigation>();
  final AtomicReference<JavaFxObject> stage = new AtomicReference<JavaFxObject>();
  final AtomicReference<JavaFxObject> view = new AtomicReference<JavaFxObject>();
  final AtomicReference<JavaFxObject> engine = new AtomicReference<JavaFxObject>();
  final AtomicReference<JavaFxObject> httpListener = new AtomicReference<JavaFxObject>();
  final AtomicBoolean initialized = new AtomicBoolean();
  final Object initLock = new Object();
  final AtomicReference<String> itemId = new AtomicReference<String>();

  BrowserContextItem() {
    itemId.set(Long.toString(currentItemId.getAndIncrement()));
  }

  void init(final JBrowserDriver driver, final BrowserContext context) {
    if (initialized.get()) {
      return;
    }
    synchronized (initLock) {
      if (!initialized.get()) {
        SettingsManager.register(stage, view,
            context.settings, context.statusCode);
        engine.set(view.get().call("getEngine"));
        context.settingsId.set(Long.parseLong(
            engine.get().call("getUserAgent").toString()));
        context.robot.set(new Robot(context));
        window.set(new Window(stage, context.settingsId.get()));
        context.keyboard.set(new Keyboard(context.robot));
        context.mouse.set(new Mouse(context.robot));
        navigation.set(new Navigation(
            new AtomicReference<JBrowserDriver>(driver), view, context.settingsId.get()));
        context.options.set(new Options(
            window, context.settings.get().cookieManager(), context.timeouts));
        context.capabilities.set(new Capabilities());
        Util.exec(Pause.SHORT, new Sync<Object>() {
          @Override
          public Object perform() {
            httpListener.set(JavaFx.getNew(DynamicHttpListener.class, context.settingsId.get(),
                context.statusCode, context.timeouts.get().getPageLoadTimeoutObjMS(),
                context.settingsId.get()));
            JavaFx.getStatic(Accessor.class, context.settingsId.get()).
                call("getPageFor", view.get().call("getEngine")).
                call("addLoadListenerClient", httpListener.get());
            engine.get().call("setCreatePopupHandler",
                JavaFx.getNew(DynamicPopupHandler.class, context.settingsId.get(), driver, context));
            return null;
          }
        }, context.settingsId.get());
        initialized.set(true);
      }
    }
  }
}
