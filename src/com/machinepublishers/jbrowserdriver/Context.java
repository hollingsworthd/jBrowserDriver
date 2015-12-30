/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "jBrowserDriver" and "Machine Publishers" are trademarks of Machine Publishers, LLC.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License version 3 for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License version 3 along with this
 * program. If not, see http://www.gnu.org/licenses/
 * 
 * For general details about how to investigate and report license violations, please see
 * https://www.gnu.org/licenses/gpl-violation.html and email the author, ops@machinepublishers.com
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
  final AtomicReference<Robot> robot = new AtomicReference<Robot>();
  final AtomicInteger statusCode = new AtomicInteger(-1);
  final AtomicLong latestThread = new AtomicLong();
  final AtomicLong curThread = new AtomicLong();
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
        } catch (RemoteException e) {
          LogsServer.instance().exception(e);
        }
      }
    }
  }

  ContextItem item() {
    return items.get(current);
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
