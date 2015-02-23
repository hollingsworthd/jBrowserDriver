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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

class BrowserContext {
  private final Map<String, BrowserContextItem> itemMap = new LinkedHashMap<String, BrowserContextItem>();
  private final List<BrowserContextItem> items = new ArrayList<BrowserContextItem>();
  private static final Object lastWindowLock = new Object();
  private static JavaFxObject lastWindow;
  private static long lastSettingsId;
  private int current = 0;
  private final Object lock = new Object();

  BrowserContext() {
    BrowserContextItem newContext = new BrowserContextItem();
    items.add(newContext);
    itemMap.put(newContext.itemId.get(), newContext);
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

  void init(JBrowserDriver driver) {
    item().init(driver, this);
    closeLastWindow();
  }

  BrowserContextItem item() {
    synchronized (lock) {
      return items.get(current);
    }
  }

  BrowserContextItem item(String handle) {
    synchronized (lock) {
      return itemMap.get(handle);
    }
  }

  String itemId() {
    return Util.exec(Pause.NONE, item().statusCode, item().timeouts.get().getPageLoadTimeoutMS(),
        new Sync<String>() {
          @Override
          public String perform() {
            synchronized (lock) {
              return item().itemId.get();
            }
          }
        }, item().settingsId.get());
  }

  Set<String> itemIds() {
    return Util.exec(Pause.NONE, item().statusCode, item().timeouts.get().getPageLoadTimeoutMS(),
        new Sync<Set<String>>() {
          @Override
          public Set<String> perform() {
            synchronized (lock) {
              return new HashSet<String>(itemMap.keySet());
            }
          }
        }, item().settingsId.get());
  }

  BrowserContextItem spawn(JBrowserDriver driver) {
    synchronized (lock) {
      BrowserContextItem newContext = new BrowserContextItem();
      newContext.timeouts.set(item().timeouts.get());
      newContext.settings.set(item().settings.get());
      newContext.init(driver, this);
      newContext.stage.get().call("toBack");
      items.add(newContext);
      itemMap.put(newContext.itemId.get(), newContext);
      return newContext;
    }
  }

  void setCurrent(String id) {
    Util.exec(Pause.SHORT, item().statusCode, item().timeouts.get().getPageLoadTimeoutMS(),
        new Sync<Object>() {
          @Override
          public Object perform() {
            synchronized (lock) {
              current = items.indexOf(itemMap.get(id));
              item().stage.get().call("toFront");
              return null;
            }
          }
        }, item().settingsId.get());
  }

  private static void close(JavaFxObject stage, boolean isLastWindow, long settingsId) {
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
    final long settingsId = item().settingsId.get();
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          close(item().stage.get(), items.size() == 1, settingsId);
          return null;
        }
      }
    }, settingsId);
    synchronized (lock) {
      final String itemId = items.remove(current).itemId.get();
      current = 0;
      itemMap.remove(itemId);
    }
  }

  void removeItem(final String itemId) {
    final long settingsId = item(itemId).settingsId.get();
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          close(item(itemId).stage.get(), items.size() == 1, settingsId);
          return null;
        }
      }
    }, settingsId);
    synchronized (lock) {
      current = 0;
      items.remove(itemMap.remove(itemId));
    }
  }

  void removeItems() {
    final long settingsId = item().settingsId.get();
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          for (BrowserContextItem curItem : items) {
            close(curItem.stage.get(), true, settingsId);
          }
          return null;
        }
      }
    }, settingsId);
    synchronized (lock) {
      items.clear();
      itemMap.clear();
      current = 0;
    }
  }
}
