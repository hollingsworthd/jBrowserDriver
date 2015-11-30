/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | screenslicer.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the GNU Affero General Public
 * License version 3 as published by the Free Software Foundation.
 *
 * "ScreenSlicer", "jBrowserDriver", "Machine Publishers", and "automatic, zero-config web scraping"
 * are trademarks of Machine Publishers, LLC.
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

class BrowserContext {
  final AtomicBoolean initialized = new AtomicBoolean();
  final AtomicReference<Logs> logs = new AtomicReference();
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
  final AtomicLong latestThread = new AtomicLong();
  final AtomicLong curThread = new AtomicLong();
  private final Map<String, BrowserContextItem> itemMap = new LinkedHashMap<String, BrowserContextItem>();
  private final List<BrowserContextItem> items = new ArrayList<BrowserContextItem>();
  private int current = 0;
  private final Object lock = new Object();

  BrowserContext(Settings settings) {
    synchronized (lock) {
      BrowserContextItem newContext = new BrowserContextItem();
      items.add(newContext);
      itemMap.put(newContext.itemId.get(), newContext);
      this.settings.set(settings);
      settingsId.set(settings.id());
      logs.set(Logs.newInstance(settingsId.get()));
    }
  }

  void reset(JBrowserDriver driver) {
    removeItems();
    synchronized (lock) {
      statusCode.set(-1);
      BrowserContextItem newContext = new BrowserContextItem();
      newContext.init(driver, this);
      items.add(newContext);
      itemMap.put(newContext.itemId.get(), newContext);
    }
  }

  void init(final JBrowserDriver driver) {
    synchronized (lock) {
      if (!items.isEmpty()) {
        items.get(current).init(driver, this);
      }
      if (initialized.compareAndSet(false, true)) {
        targetLocator.set(new TargetLocator(driver, this));
        robot.set(new Robot(this));
        keyboard.set(new Keyboard(robot));
        mouse.set(new Mouse(robot));
        capabilities.set(new Capabilities());
      }
    }
  }

  BrowserContextItem item() {
    return items.get(current);
  }

  List<BrowserContextItem> items() {
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
              newContext.stage.get().toBack();
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
              items.get(current).stage.get().toFront();
              return null;
            }
          }
        }, settingsId.get());
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
    }, settingsId.get());
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
    }, settingsId.get());
  }

  void removeItems() {
    Util.exec(Pause.NONE, statusCode, new Sync<Object>() {
      @Override
      public Object perform() {
        synchronized (lock) {
          for (BrowserContextItem curItem : items) {
            curItem.stage.get().close();
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
