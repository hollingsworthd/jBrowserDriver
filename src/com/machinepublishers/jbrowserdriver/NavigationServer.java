/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
 * https://github.com/MachinePublishers/jBrowserDriver
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

import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.AppThread.Sync;

class NavigationServer extends RemoteObject implements NavigationRemote,
    org.openqa.selenium.WebDriver.Navigation {
  private final AtomicReference<JBrowserDriverServer> driver;
  private final Context context;

  NavigationServer(final AtomicReference<JBrowserDriverServer> driver, final Context context)
      throws RemoteException {
    this.driver = driver;
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void back() {
    AppThread.exec(context.item().statusCode, ((TimeoutsServer) driver.get().manage().timeouts()).getPageLoadTimeoutMS(),
        new Sync<Object>() {
          public Object perform() {
            try {
              context.item().view.get().getEngine().getHistory().go(-1);
            } catch (IndexOutOfBoundsException e) {
              LogsServer.instance().exception(e);
            }
            return null;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void forward() {
    AppThread.exec(context.item().statusCode, ((TimeoutsServer) driver.get().manage().timeouts()).getPageLoadTimeoutMS(),
        new Sync<Object>() {
          public Object perform() {
            try {
              context.item().view.get().getEngine().getHistory().go(1);
            } catch (IndexOutOfBoundsException e) {
              LogsServer.instance().exception(e);
            }
            return null;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void refresh() {
    AppThread.exec(context.item().statusCode, ((TimeoutsServer) driver.get().manage().timeouts()).getPageLoadTimeoutMS(),
        new Sync<Object>() {
          public Object perform() {
            context.item().view.get().getEngine().reload();
            return null;
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void to(String url) {
    driver.get().get(url);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void to(URL url) {
    driver.get().get(url.toExternalForm());
  }

}
