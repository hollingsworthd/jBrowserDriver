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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.openqa.selenium.interactions.internal.Coordinates;

class CoordinatesServer extends UnicastRemoteObject
    implements CoordinatesRemote, org.openqa.selenium.interactions.internal.Coordinates {
  final Coordinates coordinates;

  CoordinatesServer(Coordinates coordinates) throws RemoteException {
    this.coordinates = coordinates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point onScreen() {
    return coordinates.onScreen();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point remoteOnScreen() {
    return new Point(onScreen());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point inViewPort() {
    return coordinates.inViewPort();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point remoteInViewPort() {
    return new Point(inViewPort());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public org.openqa.selenium.Point onPage() {
    return coordinates.onPage();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point remoteOnPage() {
    return new Point(onPage());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getAuxiliary() {
    return coordinates.getAuxiliary();
  }
}
