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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.openqa.selenium.interactions.internal.Coordinates;

class CoordinatesServer extends UnicastRemoteObject
    implements CoordinatesRemote, org.openqa.selenium.interactions.internal.Coordinates {
  final Coordinates coordinates;

  CoordinatesServer(Coordinates coordinates) throws RemoteException {
    this.coordinates = coordinates;
  }

  @Override
  public Point onScreen() {
    return new Point(coordinates.onScreen());
  }

  @Override
  public Point inViewPort() {
    return new Point(coordinates.inViewPort());
  }

  @Override
  public Point onPage() {
    return new Point(coordinates.onPage());
  }

  @Override
  public Object getAuxiliary() {
    return coordinates.getAuxiliary();
  }
}
