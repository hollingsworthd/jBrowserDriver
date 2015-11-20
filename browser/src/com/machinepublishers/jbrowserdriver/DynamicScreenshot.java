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

import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

class DynamicScreenshot {
  static byte[] toPng(byte[] bytes, int width, int height, int bytesPerComponent) {
    DataBuffer buffer = new DataBufferByte(bytes, bytes.length);
    WritableRaster raster = Raster.createInterleavedRaster(buffer, width, height,
        bytesPerComponent * width, bytesPerComponent, new int[] { 0, 1, 2 },
        (Point) null);
    ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(),
        false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
    BufferedImage image = new BufferedImage(cm, raster, true, null);

    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {}
      }
    }
  }
}
