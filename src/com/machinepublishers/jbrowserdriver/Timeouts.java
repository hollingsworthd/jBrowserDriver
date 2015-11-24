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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class Timeouts implements org.openqa.selenium.WebDriver.Timeouts {
  private AtomicLong implicit = new AtomicLong();
  private AtomicLong load = new AtomicLong();
  private AtomicLong script = new AtomicLong();

  Timeouts() {}

  @Override
  public Timeouts implicitlyWait(long duration, TimeUnit unit) {
    implicit.set(unit.convert(duration, TimeUnit.MILLISECONDS));
    return this;
  }

  @Override
  public Timeouts pageLoadTimeout(long duration, TimeUnit unit) {
    load.set(unit.convert(duration, TimeUnit.MILLISECONDS));
    return this;
  }

  @Override
  public Timeouts setScriptTimeout(long duration, TimeUnit unit) {
    script.set(unit.convert(duration, TimeUnit.MILLISECONDS));
    return this;
  }

  long getImplicitlyWaitMS() {
    return implicit.get();
  }

  long getPageLoadTimeoutMS() {
    return load.get();
  }

  long getScriptTimeoutMS() {
    return script.get();
  }

  AtomicLong getImplicitlyWaitObjMS() {
    return implicit;
  }

  AtomicLong getPageLoadTimeoutObjMS() {
    return load;
  }

  AtomicLong getScriptTimeoutObjMS() {
    return script;
  }

}
