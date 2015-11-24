package com.machinepublishers.jbrowserdriver;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JavaFxRemote extends Remote {

  JavaFxObjectRemote getNew(String type, Long id, Object... params) throws RemoteException;

  JavaFxObjectRemote getStatic(String type, Long id) throws RemoteException;

  void close(long settingsId) throws RemoteException;
}
