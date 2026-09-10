package com.group2.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.group2.server.ServerInterface;

public class Client {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("server");
            System.out.println(server.Add(50, 20));
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
