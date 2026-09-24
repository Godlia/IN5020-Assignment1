package com.group2.proxy;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.group2.server.ServerInterface;


public class Proxy implements ProxyInterface{
    boolean VERBOSE = true;
    public static void main(String[] args) throws RemoteException, NotBoundException {
            try {
                Registry registry = LocateRegistry.createRegistry(1099);
                Proxy proxy = new Proxy();
                ProxyInterface proxyStub = (ProxyInterface) UnicastRemoteObject.exportObject(proxy, 0);
                registry.bind("proxy", proxyStub);
            } catch (RemoteException | AlreadyBoundException e) {
                e.printStackTrace();
            }
    }

    private static final int MAX_WAITING_QUEUE_LENGTH = 18;

    // Table of registered servers, in registration order.
    private ServerInfo[] serverList = new ServerInfo[0];
    private final Map<ServerInfo, Integer> assignmentCounts = new HashMap<>();
    private final ExecutorService queueRefreshExecutor = Executors.newCachedThreadPool();
    
    @Override
    public synchronized void RegisterServer(ServerAdress serverAdress){
        serverList = java.util.Arrays.copyOf(serverList, serverList.length + 1);
        ServerInfo server = new ServerInfo(serverAdress, serverList.length);
        serverList[serverList.length - 1] = server;
        assignmentCounts.put(server, 0);
        // Print the server list for debugging purposes
        if (VERBOSE) {
            System.out.println("Registered servers:");
            for (ServerInfo registeredServer : serverList){
                ServerAdress severAdress = registeredServer.getServerAdress();
                System.out.println("Server Name: " + severAdress.getServerName() + ", Zone: " + registeredServer.getZone() + ", IP Address: " + severAdress.getIpAddress() + ", Port: " + severAdress.getPort());
            }
        }
    };

    @Override
    public ServerAdress RequestServer(int zone){
        ServerInfo server;
        synchronized (this) {
            server = PriorityAlgorithm(zone);
            if (server == null) {
                return null;
            }

            int assignments = assignmentCounts.merge(server, 1, Integer::sum);
            if (assignments >= MAX_WAITING_QUEUE_LENGTH) {
                assignmentCounts.put(server, 0);
                refreshQueueLengthAsync(server);
            }
        }

        return server.getServerAdress();
    };

    private ServerInfo PriorityAlgorithm(int zone){
        if (serverList.length == 0) {
            return null;
        }

        int effectiveZone = findClockwiseZone(zone);
        ServerInfo sameZone = findServerInZone(effectiveZone);
        if (sameZone == null) {
            return null;
        }

        if (sameZone.getQueLength() < MAX_WAITING_QUEUE_LENGTH) {
            return sameZone;
        }

        ServerInfo best = null;
        for (ServerInfo candidate : serverList) {
            if (candidate.getQueLength() >= MAX_WAITING_QUEUE_LENGTH) {
                continue;
            }

            if (best == null || candidate.getQueLength() < best.getQueLength()
                    || (candidate.getQueLength() == best.getQueLength()
                    && clockwiseDistance(effectiveZone, candidate.getZone())
                    < clockwiseDistance(effectiveZone, best.getZone()))) {
                best = candidate;
            }
        }

        // When every server is overloaded, return the same-zone server even though its queue is overloaded.
        return best != null ? best : sameZone;
    }

    private ServerInfo findServerInZone(int zone) {
        for (ServerInfo server : serverList) {
            if (server.getZone() == zone) {
                return server;
            }
        }
        return null;
    }

    private int findClockwiseZone(int zone) {
        int closestDistance = Integer.MAX_VALUE;
        int closestZone = -1;
        for (ServerInfo server : serverList) {
            int distance = clockwiseDistance(zone, server.getZone());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestZone = server.getZone();
            }
        }
        return closestZone;
    }

    private int clockwiseDistance(int fromZone, int toZone) {
        int highestZone = 0;
        for (ServerInfo server : serverList) {
            highestZone = Math.max(highestZone, server.getZone());
        }
        if (highestZone == 0) {
            return Integer.MAX_VALUE;
        }

        int from = Math.floorMod(fromZone - 1, highestZone) + 1; 
        return Math.floorMod(toZone - from, highestZone); // Calculate clockwise distance considering wrap-around
    }

    private void refreshQueueLengthAsync(ServerInfo server) {
        queueRefreshExecutor.submit(() -> {
            ServerAdress address = server.getServerAdress();
            try {
                // Assumption: the registered server exposes getQueLength() through
                // its RMI binding and returns its current waiting-list length.
                Registry registry = LocateRegistry.getRegistry(address.getIpAddress(), address.getPort());
                ServerInterface serverStub = (ServerInterface) registry.lookup(address.getServerName());
                int queueLength = serverStub.getQueLength();
                synchronized (this) {
                    server.setQueLength(queueLength);
                }
            } catch (RemoteException | NotBoundException | ClassCastException exception) {
                System.err.println("Could not refresh queue length for "
                        + address.getServerName() + ": " + exception.getMessage());
            }
        });
    }

}
