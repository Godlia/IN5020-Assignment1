package com.group2.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote{
    int Add(int num1,int num2) throws RemoteException;
    
    int getPopulationOfCountry(String countryCode) throws RemoteException;
    int getNumberOfCities(String countryCode, int threshold, String comp) throws RemoteException;
    int getNumberofCountries(int citycount, int threshold, String comp) throws RemoteException;
    int getNumberofCountriesMM(int citycount, int minpopulation, int maxpopulation) throws RemoteException;
}
