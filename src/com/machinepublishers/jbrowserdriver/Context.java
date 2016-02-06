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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.w3c.dom.html.HTMLElement;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

class Context {
  final AtomicBoolean initialized = new AtomicBoolean();
  final AtomicReference<TimeoutsServer> timeouts = new AtomicReference<TimeoutsServer>();
  final AtomicReference<TargetLocatorServer> targetLocator = new AtomicReference<TargetLocatorServer>();
  final AtomicReference<OptionsServer> options = new AtomicReference<OptionsServer>();
  final AtomicReference<KeyboardServer> keyboard = new AtomicReference<KeyboardServer>();
  final AtomicReference<MouseServer> mouse = new AtomicReference<MouseServer>();
  final AtomicReference<CapabilitiesServer> capabilities = new AtomicReference<CapabilitiesServer>();
  final AtomicReference<NavigationServer> navigation = new AtomicReference<NavigationServer>();
  final AtomicReference<AlertServer> alert = new AtomicReference<AlertServer>();
  final AtomicReference<Robot> robot = new AtomicReference<Robot>();
  final AtomicInteger statusCode = new AtomicInteger(-1);
  final AtomicLong latestThread = new AtomicLong();
  final AtomicLong curThread = new AtomicLong();
  final AtomicReference<HTMLElement> frame = new AtomicReference<HTMLElement>(); //TODO support iframes
  private final Map<String, ContextItem> itemMap = new LinkedHashMap<String, ContextItem>();
  private final List<ContextItem> items = new ArrayList<ContextItem>();
  private int current = 0;
  private final Object lock = new Object();

  Context(Settings settings) {
    synchronized (lock) {
      ContextItem newContext = new ContextItem();
      items.add(newContext);
      itemMap.put(newContext.itemId.get(), newContext);
      try {
        timeouts.set(new TimeoutsServer());
        alert.set(new AlertServer(timeouts));
      } catch (RemoteException e) {
        LogsServer.instance().exception(e);
      }
    }
  }

  void reset(JBrowserDriverServer driver) {
    removeItems();
    synchronized (lock) {
      statusCode.set(-1);
      ContextItem newContext = new ContextItem();
      newContext.init(driver, this);
      items.add(newContext);
      itemMap.put(newContext.itemId.get(), newContext);
    }
  }

  void init(final JBrowserDriverServer driver) {
    synchronized (lock) {
      if (!items.isEmpty()) {
        items.get(current).init(driver, this);
      }
      if (initialized.compareAndSet(false, true)) {
        robot.set(new Robot(this));
        try {
          targetLocator.set(new TargetLocatorServer(driver, this));
          keyboard.set(new KeyboardServer(robot));
          mouse.set(new MouseServer(robot));
          capabilities.set(new CapabilitiesServer());
          navigation.set(new NavigationServer(
              new AtomicReference<JBrowserDriverServer>(driver), this, statusCode));
          options.set(new OptionsServer(this, SettingsManager.settings().cookieStore(), timeouts));
        } catch (RemoteException e) {
          LogsServer.instance().exception(e);
        }
      }
    }
  }

  ContextItem item() {
    if (current < items.size()) {
      return items.get(current);
    }
    return null;
  }

  List<ContextItem> items() {
    return items;
  }

  String itemId() {
    return Util.exec(Pause.NONE, statusCode,
        new Sync<String>() {
          @Override
          public String perform() {
            synchronized (lock) {
              return items.get(current).itemId.get();
            }
          }
        });
  }

  Set<String> itemIds() {
    return Util.exec(Pause.NONE, statusCode,
        new Sync<Set<String>>() {
          @Override
          public Set<String> perform() {
            synchronized (lock) {
              return new LinkedHashSet<String>(itemMap.keySet());
            }
          }
        });
  }

  ContextItem spawn(final JBrowserDriverServer driver) {
    final Context thisObj = this;
    return Util.exec(Pause.SHORT, statusCode,
        new Sync<ContextItem>() {
          @Override
          public ContextItem perform() {
            synchronized (lock) {
              ContextItem newContext = new ContextItem();
              newContext.init(driver, thisObj);
              newContext.stage.get().toBack();
              items.add(newContext);
              itemMap.put(newContext.itemId.get(), newContext);
              return newContext;
            }
          }
        });
  }

  void setCurrent(final String id) {
    Util.exec(Pause.SHORT, statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            synchronized (lock) {
              current = items.indexOf(itemMap.get(id));
              items.get(current).stage.get().toFront();
              return null;
            }
          }
        });
  }

  void removeItem() {
    Util.exec(Pause.NONE, statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          items.get(current).stage.get().close();
          itemMap.remove(items.remove(current).itemId.get());
          current = 0;
          return null;
        }
      }
    });
  }

  void removeItem(final String itemId) {
    Util.exec(Pause.NONE, statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          itemMap.remove(itemId).stage.get().close();
          items.remove(itemId);
          current = 0;
          return null;
        }
      }
    });
  }

  void removeItems() {
    Util.exec(Pause.NONE, statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          for (ContextItem curItem : items) {
            curItem.stage.get().close();
          }
          items.clear();
          itemMap.clear();
          current = 0;
          return null;
        }
      }
    });
  }
}
