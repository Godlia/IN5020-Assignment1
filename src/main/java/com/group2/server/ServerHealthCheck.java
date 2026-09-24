package com.group2.server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class ServerHealthCheck {
    private ServerHealthCheck() {
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            registry.lookup("server");
            System.exit(0);
        } catch (RemoteException | NotBoundException exception) {
            System.exit(1);
        }
    }
}
