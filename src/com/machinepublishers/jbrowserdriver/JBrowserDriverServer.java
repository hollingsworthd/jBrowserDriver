package com.machinepublishers.jbrowserdriver;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class JBrowserDriverServer extends UnicastRemoteObject implements JBrowserDriverRemote {
  protected JBrowserDriverServer() throws RemoteException {
    super();
  }

  private static final Registry registry;

  static {
    Registry registryTmp = null;
    try {
      registryTmp = LocateRegistry.createRegistry(9012);
      registryTmp.rebind("JBrowserDriverServer", new JBrowserDriverServer());
    } catch (Throwable t) {
      Logs.logsFor(1l).exception(t);
    }
    registry = registryTmp;
  }
}
