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
package com.machinepublishers.jbrowserdriver.config;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.Dimension;

public class BrowserProperties {
  private final String script;
  private final Dimension size;

  public BrowserProperties() {
    this(true, new Dimension(1366, 768), new HashMap<String, Object>());
  }

  public BrowserProperties(boolean canvasProtection, Dimension size, Map<String, Object> navigator) {
    this.size = size;
    StringBuilder builder = new StringBuilder();
    if (canvasProtection) {
      builder.append("Object.defineProperty(HTMLCanvasElement.prototype, "
          + "'toBlob', {value:function(){return undefined;}});");
      builder.append("Object.defineProperty(HTMLCanvasElement.prototype, "
          + "'toDataURL', {value:function(){return undefined;}});");
      builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
          + "'createImageData', {value:function(){return undefined;}});");
      builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
          + "'getImageData', {value:function(){return undefined;}});");
      builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
          + "'measureText', {value:function(){return undefined;}});");
      builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
          + "'isPointInPath', {value:function(){return undefined;}});");
      builder.append("Object.defineProperty(CanvasRenderingContext2D.prototype, "
          + "'isPointInStroke', {value:function(){return undefined;}});");
    }
    if (size != null) {
      builder.append("Object.defineProperty(window,'outerWidth',{value:" + size.getWidth() + "});");
      builder.append("Object.defineProperty(window,'outerHeight',{value:" + size.getHeight() + "});");
      builder.append("Object.defineProperty(window,'innerWidth',{value:" + size.getWidth() + "});");
      builder.append("Object.defineProperty(window,'innerHeight',{value:" + size.getHeight() + "});");
      builder.append("Object.defineProperty(window,'screenX',{value: 0});");
      builder.append("Object.defineProperty(window,'screenY',{value: 0});");

      builder.append("Object.defineProperty(window,'screen',");
      builder.append("{value:{");
      builder.append("width:" + size.getWidth());
      builder.append(",height:" + size.getHeight());
      builder.append(",availWidth:" + size.getWidth());
      builder.append(",availHeight:" + size.getHeight());
      builder.append(",availLeft: 0");
      builder.append(",availTop: 0");
      builder.append(",pixelDepth: 24");
      builder.append(",colorDepth: 24");
      builder.append(",orientation: undefined");
      builder.append(",left: 0");
      builder.append(",top: 0");
      builder.append("}});");
    }
    if (navigator != null) {
      //TODO
      builder.append("Object.defineProperty(window,'navigator',{value:{}});");
    }
    this.script = builder.toString();
  }

  public Dimension size() {
    return size;
  }

  public String script() {
    return script;
  }

  @Override
  public String toString() {
    return script();
  }
}
