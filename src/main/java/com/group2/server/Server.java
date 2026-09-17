package com.group2.server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server implements ServerInterface{

    @Override
    public int Add(int num1, int num2) {
        return num1 + num2;
    }
    
    public static void main(String[] args){
        try {
            Registry registry = LocateRegistry.getRegistry();
            Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub);


        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }

    }
    @Override
    public int getPopulationOfCountry(String countryCode) {
        return 0;
    }

    @Override
    public int getNumberOfCities(String countryCode, int threshold, String comp) {
        return 1;
    }

    @Override
    public int getNumberofCountries(int citycount, int threshold, String comp) {
        return 1;
    }

    @Override
    public int getNumberofCountriesMM(int citycount, int minpopulation, int maxpopulation) {
        return 1;
    }


}
