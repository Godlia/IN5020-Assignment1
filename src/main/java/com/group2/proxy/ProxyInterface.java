package com.group2.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.group2.utils.serverAdress.ServerAdress;

public interface ProxyInterface extends Remote{
    void RegisterServer(ServerAdress serverAdress) throws RemoteException; // Intended for Server
    ServerAdress RequestServer(int zone) throws RemoteException; // Intended for Client
}
