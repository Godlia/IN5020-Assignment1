package com.group2.server;

import java.nio.file.Path;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;

import com.group2.proxy.ProxyInterface;
import com.group2.utils.serverAdress.ServerAdress;



public class Server implements ServerInterface {

    private final ServerRepository repository;

    public Server() throws SQLException {
        this.repository = new ServerRepository();
        try {
            repository.importCities(Path.of("exercise_1_dataset.csv"));
        } catch (Exception ex) {
            System.getLogger(Server.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry();
            Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 1099);
            registry.bind("server", serverStub);
            int zone = Integer.parseInt(args[1]);
            String serverHost = "server-zone" + zone;
            registerWithProxy(serverHost, 1099, "server");

            
        } catch (RemoteException | AlreadyBoundException | SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public long getPopulationofCountry(String countryName) {
        try {
            long result = repository.getPopulationOfCountry(countryName);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getNumberofCities(String countryName, int threshold, String comp) {
        try {
            int result = repository.getNumberOfCitiesFiltered(countryName, threshold, comp);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getNumberofCountries(int citycount, int threshold, String comp) {
        try {
            return repository.getNumberofCountries(citycount, threshold, comp);
        } catch (SQLException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not count countries", exception);
        }
    }

    @Override
    public int getNumberofCountriesMM(int citycount, int minpopulation, int maxpopulation) {
        try {
            return repository.getNumberofCountriesMM(citycount, minpopulation, maxpopulation);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not count countries", exception);
        }
    }

    @Override
    public int getQueLength() {
        return 0; // NOTE: Needs to be implemented
    }

    private static void registerWithProxy(String serverHost, int serverPort, String serverName) {
        String proxyHost = "proxy";
        int proxyPort = 1099;
        String proxyName = "proxy";
        try {
            Registry registry = LocateRegistry.getRegistry(proxyHost, proxyPort);
            ProxyInterface proxyStub = (ProxyInterface) registry.lookup(proxyName);
            ServerAdress serverAdress = new ServerAdress(serverHost, serverPort, serverName);
            proxyStub.RegisterServer(serverAdress);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

}
