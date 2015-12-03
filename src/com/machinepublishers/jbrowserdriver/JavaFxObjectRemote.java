package com.machinepublishers.jbrowserdriver;

import java.rmi.RemoteException;

public interface JavaFxObjectRemote {
  Object unwrap() throws RemoteException;

  boolean is(Class<?> type) throws RemoteException;

  JavaFxObjectRemote field(String fieldName) throws RemoteException;

  JavaFxObjectRemote call(String methodName, Object... params) throws RemoteException;

}
