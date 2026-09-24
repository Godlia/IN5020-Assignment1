package com.group2.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote{
    long getPopulationofCountry(String countryName, int requestedZone) throws RemoteException;
    int getNumberofCities(String countryName, int threshold, String comp, int requestedZone)
        throws RemoteException;
    int getNumberofCountries(int citycount, int threshold, String comp, int requestedZone)
        throws RemoteException;
    int getNumberofCountriesMM(int citycount, int minpopulation, int maxpopulation, int requestedZone)
        throws RemoteException;
    int getQueueLength() throws RemoteException;
}
