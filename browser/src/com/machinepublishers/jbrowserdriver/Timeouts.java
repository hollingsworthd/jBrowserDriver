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

import java.util.concurrent.TimeUnit;

public class Timeouts implements org.openqa.selenium.WebDriver.Timeouts {
  private volatile long implicit;
  private volatile long load;
  private volatile long script;

  @Override
  public Timeouts implicitlyWait(long duration, TimeUnit unit) {
    implicit = unit.convert(duration, TimeUnit.MILLISECONDS);
    return this;
  }

  @Override
  public Timeouts pageLoadTimeout(long duration, TimeUnit unit) {
    load = unit.convert(duration, TimeUnit.MILLISECONDS);
    return this;
  }

  @Override
  public Timeouts setScriptTimeout(long duration, TimeUnit unit) {
    script = unit.convert(duration, TimeUnit.MILLISECONDS);
    return this;
  }

  public long getImplicitlyWaitMS() {
    return implicit;
  }

  public long getPageLoadTimeoutMS() {
    return load;
  }

  public long getScriptTimeoutMS() {
    return script;
  }

}
