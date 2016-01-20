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

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.event.EventHandler;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEvent;
import javafx.util.Callback;

class DialogHandler {
  private static final String NO_TEXT_VALUE = "jbrowserdriver-internal-no-text-value";
  private final Object lock = new Object();
  private final AtomicReference<TimeoutsServer> timeouts;
  private final AtomicReference<String> text = new AtomicReference<String>(NO_TEXT_VALUE);
  private final LinkedList<String> inputs = new LinkedList<String>();
  private final AtomicInteger dismissQueue = new AtomicInteger();
  private final AtomicInteger acceptQueue = new AtomicInteger();
  private final AlertHandler alertHandler = new AlertHandler();
  private final ConfirmHandler confirmHandler = new ConfirmHandler();
  private final PromptHandler promptHandler = new PromptHandler();

  DialogHandler(AtomicReference<TimeoutsServer> timeouts) {
    this.timeouts = timeouts;
  }

  void listen(ContextItem item) {
    item.engine.get().setOnAlert(alertHandler);
    item.engine.get().setConfirmHandler(confirmHandler);
    item.engine.get().setPromptHandler(promptHandler);
  }

  void dismiss() {
    synchronized (lock) {
      text.set(NO_TEXT_VALUE);
      dismissQueue.incrementAndGet();
      lock.notifyAll();
    }
  }

  void accept() {
    synchronized (lock) {
      text.set(NO_TEXT_VALUE);
      acceptQueue.incrementAndGet();
      lock.notifyAll();
    }
  }

  String text() {
    synchronized (lock) {
      if (text.get() == NO_TEXT_VALUE) {
        try {
          lock.wait(timeouts.get().getScriptTimeoutMS());
        } catch (InterruptedException e) {}
      }
      return text.get() == NO_TEXT_VALUE ? null : text.get();
    }
  }

  void sendKeys(String text) {
    synchronized (lock) {
      inputs.add(text);
    }
  }

  private final class AlertHandler implements EventHandler<WebEvent<String>> {
    @Override
    public void handle(WebEvent<String> event) {
      synchronized (lock) {
        text.set(event.getData());
        lock.notifyAll();
        while (true) {
          try {
            if (dismissQueue.get() > 0) {
              dismissQueue.decrementAndGet();
              break;
            }
            if (acceptQueue.get() > 0) {
              acceptQueue.decrementAndGet();
              break;
            }
            lock.wait(timeouts.get().getScriptTimeoutMS());
          } catch (InterruptedException e) {}
        }
        text.set(NO_TEXT_VALUE);
      }
    }
  }

  private final class ConfirmHandler implements Callback<String, Boolean> {
    @Override
    public Boolean call(String param) {
      boolean accept = false;
      synchronized (lock) {
        text.set(param);
        lock.notifyAll();
        while (true) {
          try {
            if (dismissQueue.get() > 0) {
              dismissQueue.decrementAndGet();
              accept = false;
              break;
            }
            if (acceptQueue.get() > 0) {
              acceptQueue.decrementAndGet();
              accept = true;
              break;
            }
            lock.wait(timeouts.get().getScriptTimeoutMS());
          } catch (InterruptedException e) {}
        }
        text.set(NO_TEXT_VALUE);
      }
      return accept;
    }
  }

  private final class PromptHandler implements Callback<PromptData, String> {
    @Override
    public String call(PromptData param) {
      boolean accept = false;
      synchronized (lock) {
        text.set(param.getMessage());
        lock.notifyAll();
        while (true) {
          try {
            if (dismissQueue.get() > 0) {
              dismissQueue.decrementAndGet();
              accept = false;
              break;
            }
            if (acceptQueue.get() > 0) {
              acceptQueue.decrementAndGet();
              accept = true;
              break;
            }
            lock.wait(timeouts.get().getScriptTimeoutMS());
          } catch (InterruptedException e) {}
        }
        text.set(NO_TEXT_VALUE);
        return accept && !inputs.isEmpty() ? inputs.removeFirst() : null;
      }
    }
  }
}
