package com.group2.server;

import java.nio.file.Path;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import com.group2.cache.Cache;
import com.group2.cache.CacheKey;
import com.group2.cache.CacheType;
import com.group2.cache.EvictionPolicy;
import com.group2.proxy.ProxyInterface;
import com.group2.proxy.ServerAdress;




public class Server implements ServerInterface {

    private static final String CACHE_TYPE = resolveCacheType();

    private final ServerRepository repository;
    private final ServerRequestQueue requestQueue;
    private final boolean cacheEnabled = "FIFO".equals(CACHE_TYPE) || "LRU".equals(CACHE_TYPE);
    private final Cache<CacheKey, Object> cache = Cache.create(CacheType.SERVER,
            EvictionPolicy.valueOf(cacheEnabled ? CACHE_TYPE : "LRU"));

    private static String resolveCacheType() {
        String cacheType = System.getenv("CACHE_TYPE");
        if (cacheType == null || cacheType.isBlank()) {
            String cacheMode = System.getenv().getOrDefault("CACHE_MODE", "NAIVE");
            cacheType = "SERVER".equalsIgnoreCase(cacheMode)
                    ? System.getenv().getOrDefault("CACHE_POLICY", "LRU")
                    : "NAIVE";
        }
        return cacheType.toUpperCase();
    }

    public Server() throws SQLException {
        this.repository = new ServerRepository();
        this.requestQueue = new ServerRequestQueue();
        try {
            repository.importCities(Path.of("exercise_1_dataset.csv"));
        } catch (Exception ex) {
            System.getLogger(Server.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    public static void main(String[] args) {
        try {
            String serverHost = System.getenv().getOrDefault("SERVER_HOST", "localhost");
            String boundName = "server";

            Registry registry = LocateRegistry.createRegistry(1099);
            Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind(boundName, serverStub);
            int zone = registerWithProxy(serverHost, 1099, boundName);
            server.requestQueue.setServerZone(zone);
            System.out.println("Assigned zone " + zone + " to " + serverHost + ".");
        } catch (RemoteException | AlreadyBoundException | NotBoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getPopulationofCountry(String countryName, int requestedZone) {
        try {
            Long result;
            // Check in cache first
            CacheKey cacheKey = CacheKey.of("getPopulationofCountry", countryName);
            result = tryToGetFromCash(cacheKey);
            if (result != null) {
                return result;
            }

            // Fall through
            // Use requestQueue
            result = requestQueue.submit(
                requestedZone,
                () -> repository.getPopulationOfCountry(countryName)
            );
            // Post to cache
            cache.put(cacheKey, result);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getNumberofCities(String countryName, int threshold, String comp, int requestedZone) {
        try {
            Integer result;
            // Check in cache first
            CacheKey cacheKey = CacheKey.of("getNumberofCities", countryName, threshold, comp);
            result = tryToGetFromCash(cacheKey);
            if (result != null) {
                return result;
            }

            // Fall through
            // Use requestQueue
            result = requestQueue.submit(
                requestedZone,
                () -> repository.getNumberOfCitiesFiltered(countryName, threshold, comp)
            );
            // Post to cache
            cache.put(cacheKey, result);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getNumberofCountries(int citycount, int threshold, String comp, int requestedZone) {
        try {
            Integer result;
            // Check in cache first
            CacheKey cacheKey = CacheKey.of("getNumberofCountries", citycount, threshold, comp);
            result = tryToGetFromCash(cacheKey);
            if (result != null) {
                return result;
            }

            // Fall through
            // Use requestQueue
            try {
                result = requestQueue.submit(
                    requestedZone,
                    () -> {
                            return repository.getNumberofCountries(citycount, threshold, comp);
                        }
                    );
            } catch (SQLException | IllegalArgumentException e) {
                throw new IllegalStateException("Could not count countries", e);
            }
            // Post to cache
            cache.put(cacheKey, result);

            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Could not count countries", e);
        }
    }

    @Override
    public int getNumberofCountriesMM(int citycount, int minpopulation, int maxpopulation, int requestedZone) {
        try {
            Integer result;
            // Check in cache first
            CacheKey cacheKey = CacheKey.of("getNumberofCountriesMM", citycount, minpopulation, maxpopulation);
            result = tryToGetFromCash(cacheKey);
            if (result != null) {
                return result;
            }

            // Fall through
            // Use requestQueue
            try {
                result = requestQueue.submit(
                    requestedZone,
                    () -> {
                            return repository.getNumberofCountriesMM(citycount, minpopulation, maxpopulation);
                        }
                    );
            } catch (SQLException | IllegalArgumentException e) {
                throw new IllegalStateException("Could not count countries", e);
            }
            // Post to cache
            cache.put(cacheKey, result);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getQueueLength() {
        return requestQueue.size();
    }

    private static int registerWithProxy(String serverHost, int serverPort, String serverName)
            throws RemoteException, NotBoundException {
        String proxyHost = "proxy";
        int proxyPort = 1099;
        String proxyName = "proxy";
        Registry registry = LocateRegistry.getRegistry(proxyHost, proxyPort);
        ProxyInterface proxyStub = (ProxyInterface) registry.lookup(proxyName);
        ServerAdress serverAdress = new ServerAdress(serverHost, serverPort, serverName);
        int zone = proxyStub.RegisterServer(serverAdress);
        System.out.println("Registered server on " + serverHost + ":" + serverPort + " with proxy.");
        return zone;
    }

    private <T> T tryToGetFromCash(CacheKey key) throws Exception {
        // Option 1: if caching is turned off
        if (!cacheEnabled) {
            return null;
        }

        // Option 2: caching is on, and the answer is in the cache
        Object existing = cache.get(key);

        if (existing != null) {
            @SuppressWarnings("unchecked")
            T result = (T) existing;
            return result;
        }

        return null;
    }

}
