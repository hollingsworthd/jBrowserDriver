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
  private int current = 0;
  private final Object lock = new Object();

  BrowserContext() {
    BrowserContextItem newContext = new BrowserContextItem();
    items.add(newContext);
    itemMap.put(newContext.itemId.get(), newContext);
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
              return null;
            }
          }
        }, item().settingsId.get());
  }

  void removeItem() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          item().stage.get().call("close");
          return null;
        }
      }
    }, item().settingsId.get());
    synchronized (lock) {
      item().close();
      final String itemId = items.remove(current).itemId.get();
      current = 0;
      itemMap.remove(itemId);
    }
  }

  void removeItem(final String itemId) {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          item(itemId).stage.get().call("close");
          return null;
        }
      }
    }, item(itemId).settingsId.get());
    synchronized (lock) {
      item(itemId).close();
      current = 0;
      items.remove(itemMap.remove(itemId));
    }
  }

  void removeItems() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          for (BrowserContextItem curItem : items) {
            curItem.stage.get().call("close");
            curItem.close();
          }
          return null;
        }
      }
    }, item().settingsId.get());
    synchronized (lock) {
      items.clear();
      itemMap.clear();
      current = 0;
    }
  }
}
