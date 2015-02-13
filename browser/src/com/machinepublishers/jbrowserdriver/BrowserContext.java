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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class BrowserContext {
  private static final Map<String, BrowserContextItem> itemIds = new HashMap<String, BrowserContextItem>();
  private final Set<String> myItemIds = new HashSet<String>();

  private int current = 0;
  private final List<BrowserContextItem> items = new ArrayList<BrowserContextItem>();

  public BrowserContext() {
    BrowserContextItem newContext = new BrowserContextItem();
    items.add(newContext);
    myItemIds.add(newContext.windowHandle.get());
    synchronized (itemIds) {
      itemIds.put(newContext.windowHandle.get(), newContext);
    }
  }

  public synchronized BrowserContextItem current() {
    return items.get(current);
  }

  public synchronized String currentId() {
    return current().windowHandle.get();
  }

  public synchronized Set<String> ids() {
    return new HashSet<String>(myItemIds);
  }

  public synchronized BrowserContextItem spawn(JBrowserDriver driver) {
    BrowserContextItem newContext = new BrowserContextItem();
    newContext.timeouts.set(current().timeouts.get());
    newContext.settings.set(current().settings.get());
    newContext.init(driver, this);
    items.add(newContext);
    myItemIds.add(newContext.windowHandle.get());
    synchronized (itemIds) {
      itemIds.put(newContext.windowHandle.get(), newContext);
    }
    return newContext;
  }

  public synchronized void destroyCurrent() {
    final String id = items.remove(current).windowHandle.get();
    current = 0;
    myItemIds.remove(id);
    synchronized (itemIds) {
      itemIds.remove(id);
    }
  }
}
