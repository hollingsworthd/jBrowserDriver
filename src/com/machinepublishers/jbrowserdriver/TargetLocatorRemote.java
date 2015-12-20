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

import java.rmi.Remote;
import java.rmi.RemoteException;

interface TargetLocatorRemote extends Remote {
  ElementRemote activeElement() throws RemoteException;

  AlertRemote alert() throws RemoteException;

  JBrowserDriverRemote defaultContent() throws RemoteException;

  JBrowserDriverRemote frame(int arg0) throws RemoteException;

  JBrowserDriverRemote frame(String arg0) throws RemoteException;

  JBrowserDriverRemote frame(ElementRemote arg0) throws RemoteException;

  JBrowserDriverRemote parentFrame() throws RemoteException;

  JBrowserDriverRemote window(String windowHandle) throws RemoteException;
}
