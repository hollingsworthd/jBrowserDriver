/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 Machine Publishers, LLC and the jBrowserDriver contributors
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

import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.WebPage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.w3c.dom.Document;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class ContextItem {
  private static final AtomicLong currentItemId = new AtomicLong();

  final AtomicReference<WindowServer> window = new AtomicReference<WindowServer>();
  final AtomicReference<Stage> stage = new AtomicReference<Stage>();
  final AtomicReference<WebView> view = new AtomicReference<WebView>();
  final AtomicReference<WebEngine> engine = new AtomicReference<WebEngine>();
  final AtomicReference<HttpListener> httpListener = new AtomicReference<HttpListener>();
  final AtomicBoolean initialized = new AtomicBoolean();
  final AtomicReference<String> itemId = new AtomicReference<String>();
  final AtomicReference<Context> context = new AtomicReference<Context>();
  private final Frames frames = new Frames();
  private ElementServer frame;

  ContextItem() {
    itemId.set(Long.toString(currentItemId.getAndIncrement()));
  }

  ElementServer selectedFrame() {
    synchronized (frames) {
      if (frame != null
          && (!(frame.node() instanceof Document) || !frames.conatins(frame.node()))) {
        boolean foundFrame = false;
        try {
          if (frame.frameId() > 0) {
            Document doc = Accessor.getPageFor(engine.get()).getDocument(frame.frameId());
            if (doc instanceof JSObject) {
              selectFrame(new ElementServer((JSObject) doc, context.get()));
              foundFrame = true;
            }
          }
        } catch (Throwable t) {}
        if (!foundFrame) {
          deselectFrame();
        }
      }
      return frame;
    }
  }

  boolean containsFrame(JSObject doc) {
    synchronized (frames) {
      return frames.conatins(doc);
    }
  }

  org.openqa.selenium.Point selectedFrameLocation() {
    synchronized (frames) {
      ElementServer selectedFrame = selectedFrame();
      int xCoord = 0;
      int yCoord = 0;
      if (selectedFrame != null) {
        long frameId = frames.id(selectedFrame.node());
        WebPage webPage = Accessor.getPageFor(engine.get());
        List<Long> ancestors = frames.ancestors(frameId);
        ancestors.add(frameId);
        for (Long curFrameId : ancestors) {
          try {
            org.w3c.dom.Element owner = webPage.getOwnerElement(curFrameId);
            if (owner instanceof JSObject) {
              org.openqa.selenium.Point point = new ElementServer(((JSObject) owner), context.get()).getLocation();
              xCoord += point.getX();
              yCoord += point.getY();
            }
          } catch (RemoteException e) {
            Util.handleException(e);
          }
        }
      }
      return new org.openqa.selenium.Point(xCoord, yCoord);
    }
  }

  void deselectFrame() {
    synchronized (frames) {
      frame = null;
    }
  }

  void selectFrame(ElementServer frame) {
    synchronized (frames) {
      frame.setFrameId(frames.id(frame.node()));
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
      frames.add(frameId, (JSObject) webPage.getDocument(frameId),
          (JSObject) webPage.getOwnerElement(frameId), webPage.getParentFrame(frameId));
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
      this.context.set(context);
      SettingsManager.register(stage, view);
      engine.set(view.get().getEngine());
      try {
        window.set(new WindowServer(stage, context.statusCode));
        context.alert.get().listen(this);
      } catch (RemoteException e) {
        Util.handleException(e);
      }
      final ContextItem thisObject = this;
      AppThread.exec(context.statusCode, () -> {
        Settings settings = SettingsManager.settings();
        engine.get().setJavaScriptEnabled(settings.javascript());
        //If null engine uses automatic value.
        engine.get().setUserDataDirectory(context.userDataDirectory.get());
        httpListener.set(new HttpListener(thisObject,
            context.statusCode, context.timeouts.get().getPageLoadTimeoutObjMS()));
        Accessor.getPageFor(view.get().getEngine()).addLoadListenerClient(httpListener.get());
        engine.get().setCreatePopupHandler(new PopupHandler(driver, context));
        return null;
      });
    }
  }
}
