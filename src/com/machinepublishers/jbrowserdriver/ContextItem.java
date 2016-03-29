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

import com.machinepublishers.jbrowserdriver.AppThread.Pause;
import com.machinepublishers.jbrowserdriver.AppThread.Sync;
import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

class ContextItem {
  private static final AtomicLong currentItemId = new AtomicLong();

  final AtomicReference<WindowServer> window = new AtomicReference<WindowServer>();
  final AtomicReference<Stage> stage = new AtomicReference<Stage>();
  final AtomicReference<WebView> view = new AtomicReference<WebView>();
  final AtomicReference<WebEngine> engine = new AtomicReference<WebEngine>();
  final AtomicReference<HttpListener> httpListener = new AtomicReference<HttpListener>();
  final AtomicBoolean initialized = new AtomicBoolean();
  final AtomicReference<String> itemId = new AtomicReference<String>();
  private final Frames frames = new Frames();
  private ElementServer frame;

  ContextItem() {
    itemId.set(Long.toString(currentItemId.getAndIncrement()));
  }

  ElementServer selectedFrame() {
    synchronized (frames) {
      if (frame != null && !frames.conatins(frame.node())) {
        deselectFrame();
      }
      //TODO after returning this frame it might be possible for it to become invalid
      return frame;
    }
  }

  void deselectFrame() {
    synchronized (frames) {
      frame = null;
    }
  }

  void selectFrame(ElementServer frame) {
    synchronized (frames) {
      this.frame = frame;
    }
  }

  void resetFrameId(long frameId) {
    synchronized (frames) {
      frames.reset(frameId);
    }
  }

  void addFrameId(long frameId) {
    synchronized (frames) {
      WebPage webPage = Accessor.getPageFor(engine.get());
      frames.add(frameId, (JSObject) webPage.getDocument(frameId), webPage.getParentFrame(frameId));
    }
  }

  long currentFrameId() {
    synchronized (frames) {
      ElementServer selectedFrame = selectedFrame();
      long selectedId = selectedFrame == null ? 0 : frames.id(selectedFrame.node());
      return selectedId == 0 ? frames.rootId() : selectedId;
    }
  }

  void init(final JBrowserDriverServer driver, final Context context) {
    if (initialized.compareAndSet(false, true)) {
      SettingsManager.register(stage, view);
      engine.set(view.get().getEngine());
      try {
        window.set(new WindowServer(stage, context.statusCode));
        context.alert.get().listen(this);
      } catch (RemoteException e) {
        LogsServer.instance().exception(e);
      }
      final ContextItem thisObject = this;
      AppThread.exec(Pause.SHORT, context.statusCode, new Sync<Object>() {
        @Override
        public Object perform() {
          engine.get().setJavaScriptEnabled(SettingsManager.settings().javascript());
          httpListener.set(new HttpListener(thisObject,
              context.statusCode, context.timeouts.get().getPageLoadTimeoutObjMS()));
          Accessor.getPageFor(view.get().getEngine()).addLoadListenerClient(httpListener.get());
          engine.get().setCreatePopupHandler(new PopupHandler(driver, context));
          return null;
        }
      });
    }
  }
}
