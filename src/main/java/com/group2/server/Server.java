package com.group2.server;

import java.nio.file.Path;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;



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
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub);

            
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
    public boolean status() {
        return true;
    }

}
