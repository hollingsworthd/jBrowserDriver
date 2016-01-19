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
  private final AtomicReference<String> text = new AtomicReference<String>(NO_TEXT_VALUE);
  private final LinkedList<String> inputs = new LinkedList<String>();
  private final AtomicInteger dismissQueue = new AtomicInteger();
  private final AtomicInteger acceptQueue = new AtomicInteger();
  private final AlertHandler alertHandler = new AlertHandler(lock, text, dismissQueue, acceptQueue);
  private final ConfirmHandler confirmHandler = new ConfirmHandler(lock, text, dismissQueue, acceptQueue);
  private final PromptHandler promptHandler = new PromptHandler(lock, text, dismissQueue, acceptQueue, inputs);

  DialogHandler(ContextItem item) {
    item.engine.get().setOnAlert(alertHandler);
    item.engine.get().setConfirmHandler(confirmHandler);
    item.engine.get().setPromptHandler(promptHandler);
  }

  void dismiss() {
    synchronized (lock) {
      dismissQueue.incrementAndGet();
      lock.notifyAll();
    }
  }

  void accept() {
    synchronized (lock) {
      acceptQueue.incrementAndGet();
      lock.notifyAll();
    }
  }

  String text() {
    synchronized (text) {
      while (true) {
        if (text.get() == NO_TEXT_VALUE) {
          try {
            text.wait();
          } catch (InterruptedException e) {}
        } else {
          break;
        }
      }
      return text.get();
    }
  }

  void sendKeys(String text) {
    synchronized (inputs) {
      inputs.add(text);
    }
  }

  private static class AlertHandler implements EventHandler<WebEvent<String>> {
    private final Object lock;
    private final AtomicReference<String> text;
    private final AtomicInteger dismissQueue;
    private final AtomicInteger acceptQueue;

    AlertHandler(Object lock, AtomicReference<String> text, AtomicInteger dismissQueue, AtomicInteger acceptQueue) {
      this.lock = lock;
      this.text = text;
      this.dismissQueue = dismissQueue;
      this.acceptQueue = acceptQueue;
    }

    @Override
    public void handle(WebEvent<String> event) {
      synchronized (lock) {
        synchronized (text) {
          text.set(event.getData());
          text.notify();
        }
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
            lock.wait();
          } catch (InterruptedException e) {}
        }
        synchronized (text) {
          text.set(NO_TEXT_VALUE);
        }
      }
    }
  }

  private static class ConfirmHandler implements Callback<String, Boolean> {
    private final Object lock;
    private final AtomicReference<String> text;
    private final AtomicInteger dismissQueue;
    private final AtomicInteger acceptQueue;

    ConfirmHandler(Object lock, AtomicReference<String> text, AtomicInteger dismissQueue, AtomicInteger acceptQueue) {
      this.lock = lock;
      this.text = text;
      this.dismissQueue = dismissQueue;
      this.acceptQueue = acceptQueue;
    }

    @Override
    public Boolean call(String param) {
      boolean accept = false;
      synchronized (lock) {
        synchronized (text) {
          text.set(param);
          text.notify();
        }
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
            lock.wait();
          } catch (InterruptedException e) {}
        }
        synchronized (text) {
          text.set(NO_TEXT_VALUE);
        }
      }
      return accept;
    }
  }

  private static class PromptHandler implements Callback<PromptData, String> {
    private final Object lock;
    private final AtomicReference<String> text;
    private final AtomicInteger dismissQueue;
    private final AtomicInteger acceptQueue;
    private final LinkedList<String> inputs;

    PromptHandler(Object lock, AtomicReference<String> text,
        AtomicInteger dismissQueue, AtomicInteger acceptQueue, LinkedList<String> inputs) {
      this.lock = lock;
      this.text = text;
      this.dismissQueue = dismissQueue;
      this.acceptQueue = acceptQueue;
      this.inputs = inputs;
    }

    @Override
    public String call(PromptData param) {
      boolean accept = false;
      synchronized (lock) {
        synchronized (text) {
          text.set(param.getMessage());
          text.notify();
        }
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
            lock.wait();
          } catch (InterruptedException e) {}
        }
        synchronized (text) {
          text.set(NO_TEXT_VALUE);
        }
      }
      synchronized (inputs) {
        return accept && !inputs.isEmpty() ? inputs.removeFirst() : null;
      }
    }
  }
}
