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

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.web.WebView;

import com.machinepublishers.jbrowserdriver.Util.Sync;

public class Navigation implements org.openqa.selenium.WebDriver.Navigation {
  private final AtomicReference<JBrowserDriver> driver;
  private final AtomicReference<WebView> view;

  Navigation(final AtomicReference<JBrowserDriver> driver, final AtomicReference<WebView> view) {
    this.driver = driver;
    this.view = view;
  }

  @Override
  public void back() {
    Util.exec(new Sync<Object>() {
      public Object perform() {
        try {
          view.get().getEngine().getHistory().go(-1);
        } catch (IndexOutOfBoundsException e) {
          Logs.exception(e);
        }
        return null;
      }
    });
  }

  @Override
  public void forward() {
    Util.exec(new Sync<Object>() {
      public Object perform() {
        try {
          view.get().getEngine().getHistory().go(1);
        } catch (IndexOutOfBoundsException e) {
          Logs.exception(e);
        }
        return null;
      }
    });
  }

  @Override
  public void refresh() {
    Util.exec(new Sync<Object>() {
      public Object perform() {
        view.get().getEngine().reload();
        return null;
      }
    });
  }

  @Override
  public void to(String url) {
    driver.get().get(url);
  }

  @Override
  public void to(URL url) {
    driver.get().get(url.toExternalForm());
  }

}
