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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.html.HTMLFrameElement;
import org.w3c.dom.html.HTMLIFrameElement;

import com.sun.javafx.webkit.Accessor;

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
  final AtomicReference<Context> context = new AtomicReference<Context>();
  final StatusCode statusCode = new StatusCode();
  private final Object lock = new Object();
  private ElementServer frame;

  ContextItem() {
    itemId.set(Long.toString(currentItemId.getAndIncrement()));
  }

  JSObject selectedFrameDoc() {
    synchronized (lock) {
      if (frame != null && ancestors(engine.get().getDocument(), frame.node()) != null) {
        if (frame.node() instanceof HTMLIFrameElement) {
          Document doc = ((HTMLIFrameElement) frame.node()).getContentDocument();
          if (doc instanceof JSObject) {
            return (JSObject) doc;
          }
        }
        if (frame.node() instanceof HTMLFrameElement) {
          Document doc = ((HTMLFrameElement) frame.node()).getContentDocument();
          if (doc instanceof JSObject) {
            return (JSObject) doc;
          }
        }
      }
      frame = null;
      Document doc = engine.get().getDocument();
      if (doc instanceof JSObject) {
        return (JSObject) doc;
      }
      return null;
    }
  }

  boolean containsFrame(JSObject doc) {
    return engine.get().getDocument().equals(doc) || ancestors(engine.get().getDocument(), doc) != null;
  }

  private List<Node> ancestors(Document doc, JSObject targetNode) {
    if (targetNode != null) {
      List<Node> list = new ArrayList<Node>();
      try {
        List elements = new ArrayList();
        elements.addAll(new ElementServer((JSObject) doc, this).findElementsByTagName("frame"));
        elements.addAll(new ElementServer((JSObject) doc, this).findElementsByTagName("iframe"));
        for (Object cur : elements) {
          JSObject node = ((ElementServer) cur).node();
          if (node instanceof Node) {
            list.add((Node) node);
          }
        }
      } catch (RemoteException e) {
        Util.handleException(e);
      }
      for (Node cur : list) {
        Document curDoc = null;
        if (cur instanceof HTMLFrameElement) {
          curDoc = ((HTMLFrameElement) cur).getContentDocument();
        } else if (cur instanceof HTMLIFrameElement) {
          curDoc = ((HTMLIFrameElement) cur).getContentDocument();
        }
        Document targetDoc = null;
        if (targetNode instanceof HTMLFrameElement) {
          targetDoc = ((HTMLFrameElement) targetNode).getContentDocument();
        } else if (targetNode instanceof HTMLIFrameElement) {
          targetDoc = ((HTMLIFrameElement) targetNode).getContentDocument();
        } else if (targetNode instanceof Document) {
          targetDoc = (Document) targetNode;
        }
        if (curDoc.equals(targetDoc)) {
          List<Node> ancestors = new ArrayList<Node>();
          ancestors.add(cur);
          return ancestors;
        }
        List<Node> ancestors = ancestors(curDoc, targetNode);
        if (ancestors != null) {
          ancestors.add(cur);
          return ancestors;
        }
      }
    }
    return null;

  }

  org.openqa.selenium.Point selectedFrameLocation() {
    synchronized (lock) {
      int xCoord = 0;
      int yCoord = 0;
      if (frame != null) {
        List<Node> ancestors = ancestors(engine.get().getDocument(), frame.node());
        if (ancestors != null) {
          for (Node cur : ancestors) {
            try {
              if (cur instanceof JSObject) {
                org.openqa.selenium.Point point = new ElementServer((JSObject) cur, this).getLocation();
                xCoord += point.getX();
                yCoord += point.getY();
              }
            } catch (RemoteException e) {
              Util.handleException(e);
            }

          }
        }
      }
      return new org.openqa.selenium.Point(xCoord, yCoord);
    }
  }

  void deselectFrame() {
    synchronized (lock) {
      frame = null;
    }
  }

  void selectFrame(ElementServer frame) {
    synchronized (lock) {
      this.frame = null;
      if (frame != null && frame.node() instanceof Document && !frame.node().equals(engine.get().getDocument())) {
        List<Node> ancestors = ancestors(engine.get().getDocument(), frame.node());
        if (ancestors != null && !ancestors.isEmpty() && ancestors.get(0) instanceof JSObject) {
          try {
            this.frame = new ElementServer((JSObject) ancestors.get(0), this);
          } catch (RemoteException e) {
            Util.handleException(e);
          }
        }
      } else if (frame != null &&
          (frame.node() instanceof HTMLIFrameElement || frame.node() instanceof HTMLFrameElement)) {
        this.frame = frame;
      }
    }
  }

  void init(final JBrowserDriverServer driver, final Context context) {
    if (initialized.compareAndSet(false, true)) {
      this.context.set(context);
      SettingsManager.register(stage, view);
      engine.set(view.get().getEngine());
      try {
        window.set(new WindowServer(stage, statusCode));
        context.alert.get().listen(this);
      } catch (RemoteException e) {
        Util.handleException(e);
      }
      final ContextItem thisObject = this;
      AppThread.exec(statusCode, () -> {
        Settings settings = SettingsManager.settings();
        engine.get().setJavaScriptEnabled(settings.javascript());
        //If null engine uses automatic value.
        engine.get().setUserDataDirectory(context.userDataDirectory.get());
        httpListener.set(new HttpListener(thisObject,
            statusCode, context.timeouts.get().getPageLoadTimeoutObjMS()));
        httpListener.get().init();
        Accessor.getPageFor(view.get().getEngine()).addLoadListenerClient(httpListener.get());
        engine.get().setCreatePopupHandler(new PopupHandler(driver, context));
        return null;
      });
    }
  }
}
