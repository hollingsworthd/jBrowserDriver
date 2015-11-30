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

import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

import javafx.scene.web.WebView;

class Navigation implements org.openqa.selenium.WebDriver.Navigation {
  private final AtomicReference<JBrowserDriver> driver;
  private final AtomicReference<WebView> view;
  private final AtomicInteger statusCode;
  private final long settingsId;

  Navigation(final AtomicReference<JBrowserDriver> driver,
      final AtomicReference<WebView> view, final AtomicInteger statusCode, final long settingsId) {
    this.driver = driver;
    this.view = view;
    this.statusCode = statusCode;
    this.settingsId = settingsId;
  }

  @Override
  public void back() {
    Util.exec(Pause.SHORT, statusCode, ((Timeouts) driver.get().manage().timeouts()).getPageLoadTimeoutMS(),
        new Sync<Object>() {
          public Object perform() {
            try {
              view.get().getEngine().getHistory().go(-1);
            } catch (IndexOutOfBoundsException e) {
              driver.get().context.logs.get().exception(e);
            }
            return null;
          }
        }, settingsId);
  }

  @Override
  public void forward() {
    Util.exec(Pause.SHORT, statusCode, ((Timeouts) driver.get().manage().timeouts()).getPageLoadTimeoutMS(),
        new Sync<Object>() {
          public Object perform() {
            try {
              view.get().getEngine().getHistory().go(1);
            } catch (IndexOutOfBoundsException e) {
              driver.get().context.logs.get().exception(e);
            }
            return null;
          }
        }, settingsId);
  }

  @Override
  public void refresh() {
    Util.exec(Pause.SHORT, statusCode, ((Timeouts) driver.get().manage().timeouts()).getPageLoadTimeoutMS(),
        new Sync<Object>() {
          public Object perform() {
            view.get().getEngine().reload();
            return null;
          }
        }, settingsId);
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
