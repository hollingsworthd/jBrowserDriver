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

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.Platform;

public class Capabilities implements org.openqa.selenium.Capabilities {

  Capabilities() {}

  @Override
  public Map<String, ?> asMap() {
    return new HashMap<String, String>();
  }

  @Override
  public String getBrowserName() {
    return "jBrowserDriver (WebKit-based) by Machine Publishers, LLC";
  }

  @Override
  public Object getCapability(String name) {
    return null;
  }

  @Override
  public Platform getPlatform() {
    return Platform.ANY;
  }

  @Override
  public String getVersion() {
    return Runtime.class.getPackage().getImplementationVersion();
  }

  @Override
  public boolean is(String name) {
    return false;
  }

  @Override
  public boolean isJavascriptEnabled() {
    return true;
  }

}
