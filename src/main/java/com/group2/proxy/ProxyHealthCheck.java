package com.group2.proxy;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class ProxyHealthCheck {
    private ProxyHealthCheck() {
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            registry.lookup("proxy");
            System.exit(0);
        } catch (Exception exception) {
            System.exit(1);
        }
    }
}
