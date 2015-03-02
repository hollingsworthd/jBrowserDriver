/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

class BrowserContext {
  final AtomicReference<Timeouts> timeouts = new AtomicReference<Timeouts>(new Timeouts());
  final AtomicReference<TargetLocator> targetLocator = new AtomicReference<TargetLocator>();
  final AtomicReference<Options> options = new AtomicReference<Options>();
  final AtomicReference<Keyboard> keyboard = new AtomicReference<Keyboard>();
  final AtomicReference<Mouse> mouse = new AtomicReference<Mouse>();
  final AtomicReference<Capabilities> capabilities = new AtomicReference<Capabilities>();
  final AtomicReference<Robot> robot = new AtomicReference<Robot>();
  final AtomicInteger statusCode = new AtomicInteger(-1);
  final AtomicReference<Settings> settings = new AtomicReference<Settings>();
  final AtomicLong settingsId = new AtomicLong();
  private final Map<String, BrowserContextItem> itemMap = new LinkedHashMap<String, BrowserContextItem>();
  private final List<BrowserContextItem> items = new ArrayList<BrowserContextItem>();
  private static final Object lastWindowLock = new Object();
  private static JavaFxObject lastWindow;
  private static long lastSettingsId;
  private int current = 0;
  private final Object lock = new Object();

  BrowserContext() {
    synchronized (lock) {
      BrowserContextItem newContext = new BrowserContextItem();
      items.add(newContext);
      itemMap.put(newContext.itemId.get(), newContext);
    }
  }

  private static void closeLastWindow() {
    if (!Settings.headless()) {
      final long settingsId;
      final JavaFxObject window;
      synchronized (lastWindowLock) {
        if (lastWindow != null) {
          window = lastWindow;
          lastWindow = null;
          settingsId = lastSettingsId;
        } else {
          window = null;
          settingsId = 0l;
        }
      }
      if (window != null) {
        Util.exec(Pause.SHORT, new Sync<Object>() {
          @Override
          public Object perform() {
            window.call("close");
            return null;
          }
        }, settingsId);
      }
    }
  }

  void init(final JBrowserDriver driver) {
    synchronized (lock) {
      targetLocator.compareAndSet(null, new TargetLocator(driver, this));
      items.get(current).init(driver, this);
    }
    closeLastWindow();
  }

  BrowserContextItem item() {
    return Util.exec(Pause.NONE, new AtomicInteger(-1),
        new Sync<BrowserContextItem>() {
          @Override
          public BrowserContextItem perform() {
            synchronized (lock) {
              return items.get(current);
            }
          }
        }, settingsId.get());
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
        }, settingsId.get());
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
        }, settingsId.get());
  }

  BrowserContextItem spawn(final JBrowserDriver driver) {
    final BrowserContext thisObj = this;
    return Util.exec(Pause.SHORT, statusCode,
        new Sync<BrowserContextItem>() {
          @Override
          public BrowserContextItem perform() {
            synchronized (lock) {
              BrowserContextItem newContext = new BrowserContextItem();
              newContext.init(driver, thisObj);
              newContext.stage.get().call("toBack");
              items.add(newContext);
              itemMap.put(newContext.itemId.get(), newContext);
              return newContext;
            }
          }
        }, settingsId.get());
  }

  void setCurrent(final String id) {
    Util.exec(Pause.SHORT, statusCode,
        new Sync<Object>() {
          @Override
          public Object perform() {
            synchronized (lock) {
              current = items.indexOf(itemMap.get(id));
              items.get(current).stage.get().call("toFront");
              return null;
            }
          }
        }, settingsId.get());
  }

  private static void close(final JavaFxObject stage,
      final boolean isLastWindow, final long settingsId) {
    boolean close = true;
    if (!Settings.headless() && isLastWindow) {
      synchronized (lastWindowLock) {
        if (lastWindow == null) {
          lastWindow = stage;
          lastSettingsId = settingsId;
          close = false;
        }
      }
    }
    if (close) {
      stage.call("close");
    }
  }

  void removeItem() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          close(items.get(current).stage.get(), items.size() == 1, settingsId.get());
          final String itemId = items.remove(current).itemId.get();
          current = 0;
          itemMap.remove(itemId);
          return null;
        }
      }
    }, settingsId.get());
  }

  void removeItem(final String itemId) {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          close(itemMap.get(itemId).stage.get(), items.size() == 1, settingsId.get());
          current = 0;
          items.remove(itemMap.remove(itemId));
          return null;
        }
      }
    }, settingsId.get());
  }

  void removeItems() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          for (BrowserContextItem curItem : items) {
            close(curItem.stage.get(), true, settingsId.get());
          }
          items.clear();
          itemMap.clear();
          current = 0;
          return null;
        }
      }
    }, settingsId.get());
  }
}
