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

import com.machinepublishers.jbrowserdriver.Util.Sync;

class BrowserContext {
  private final Map<String, BrowserContextItem> itemMap = new LinkedHashMap<String, BrowserContextItem>();
  private final List<BrowserContextItem> items = new ArrayList<BrowserContextItem>();
  private int current = 0;

  public BrowserContext() {
    BrowserContextItem newContext = new BrowserContextItem();
    items.add(newContext);
    itemMap.put(newContext.itemId.get(), newContext);
  }

  public synchronized BrowserContextItem item() {
    return items.get(current);
  }

  public synchronized BrowserContextItem item(String handle) {
    return itemMap.get(handle);
  }

  public synchronized String itemId() {
    return item().itemId.get();
  }

  public synchronized Set<String> itemIds() {
    return new HashSet<String>(itemMap.keySet());
  }

  public synchronized BrowserContextItem spawn(JBrowserDriver driver) {
    BrowserContextItem newContext = new BrowserContextItem();
    newContext.timeouts.set(item().timeouts.get());
    newContext.settings.set(item().settings.get());
    newContext.init(driver, this);
    items.add(newContext);
    itemMap.put(newContext.itemId.get(), newContext);
    return newContext;
  }

  public synchronized void setCurrent(String id) {
    current = items.indexOf(itemMap.get(id));
    Util.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        item().stage.get().call("setFocused", true);
        return null;
      }
    }, item().settingsId.get());
  }

  public synchronized void removeItem() {
    Util.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        item().stage.get().call("close");
        return null;
      }
    }, item().settingsId.get());
    item().close();
    final String itemId = items.remove(current).itemId.get();
    current = 0;
    itemMap.remove(itemId);
  }

  public synchronized void removeItem(final String itemId) {
    Util.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        item(itemId).stage.get().call("close");
        return null;
      }
    }, item(itemId).settingsId.get());
    item(itemId).close();
    current = 0;
    items.remove(itemMap.remove(itemId));
  }

  public synchronized void removeItems() {
    Util.exec(new Sync<Object>() {
      @Override
      public Object perform() {
        for (BrowserContextItem curItem : items) {
          curItem.stage.get().call("close");
          curItem.close();
        }
        return null;
      }
    }, item().settingsId.get());
    items.clear();
    itemMap.clear();
    current = 0;
  }
}
