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
    boolean VERBOSE = Boolean.parseBoolean(System.getenv().getOrDefault("VERBOSE", "false")); // For DEBUGGING
    private static final int MAX_WAITING_QUEUE_LENGTH = 18;

    public static void main(String[] args) throws RemoteException, NotBoundException {
            try {
                // Binds the proxy to the RMI registry in its own container
                Registry registry = LocateRegistry.createRegistry(1099);
                Proxy proxy = new Proxy();
                ProxyInterface proxyStub = (ProxyInterface) UnicastRemoteObject.exportObject(proxy, 0);
                registry.bind("proxy", proxyStub);
            } catch (RemoteException | AlreadyBoundException e) {
                e.printStackTrace();
            }
    }

    // Storing the info related to retriving the servers
    private ServerInfo[] serverList = new ServerInfo[0]; 

    // Used to track so that the proxy only retrives the queue length when the server has been assigned MAX_WAITING_QUEUE_LENGTH times.
    private final Map<ServerInfo, Integer> assignmentCounts = new HashMap<>(); 

    // Used so that the queue length refreshes are done asynchronously and do not block the main thread.
    private final ExecutorService queueRefreshExecutor = Executors.newCachedThreadPool();

    
    @Override
    public synchronized int RegisterServer(ServerAdress serverAdress){

        // Appends new server to serverList and assignmenCounts
        serverList = java.util.Arrays.copyOf(serverList, serverList.length + 1);
        int zone = serverList.length;
        ServerAdress addressWithZone = new ServerAdress(
            serverAdress.getIpAddress(),
            serverAdress.getPort(),
            serverAdress.getServerName(),
            zone);
        ServerInfo server = new ServerInfo(addressWithZone, zone);
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
        return server.getZone();
    };

    @Override
    public ServerAdress RequestServer(int zone){
        ServerInfo server;
        synchronized (this) {
            // Handles everything related to desiding which zone is the right given our conditions.
            server = PriorityAlgorithm(zone);
            if (server == null) { // Failsafe in case servers don't exist yet, but should never happen because Docker compose does health checks
                return null;
            }

            int assignments = assignmentCounts.merge(server, 1, Integer::sum); // Counts upward how many times the server has been assigned.
            if (assignments >= MAX_WAITING_QUEUE_LENGTH) {
                // If threshold is reached, refresh the queue length asynchronously and reset the count.
                refreshQueueLengthAsync(server);
                assignmentCounts.put(server, 0);
            }
        }

        ServerAdress address = server.getServerAdress(); // Gets the part the client needs (with info on how to connect to the server)
        return new ServerAdress(
            address.getIpAddress(),
            address.getPort(),
            address.getServerName(),
            server.getZone());
    };

    private ServerInfo PriorityAlgorithm(int zone){
        if (serverList.length == 0) {
            return null;
        }

        // Given health check in docker-compose, this will just return the same zone, 
        // but it is there to foolproof, to use the next available server as described in the assignment.
        int effectiveZone = findClockwiseZone(zone);
        ServerInfo sameZone = findServerInZone(effectiveZone);
        if (sameZone == null) {
            return null;
        }

        // Bellow maximum queue length condition
        if (sameZone.getQueLength() < MAX_WAITING_QUEUE_LENGTH) {
            return sameZone;
        }

        // If none of the if cathes above return something, it falls through to find the shortest queue length.
        // As described in the assignment it both gets the shortest, and does in a clockwise manner.
        // It balances thouse two conditions by prioritising the shortest over all, but if two servers have the same queue length, it will choose the one that is closest in a clockwise manner.
        ServerInfo best = null;
        for (ServerInfo candidate : serverList) {
            if (candidate.getQueLength() >= MAX_WAITING_QUEUE_LENGTH) {
                continue; // Skip servers that are overloaded, as pr. assignment instructions.
            }

            boolean shouldSelectCandidate = best == null;

            if (!shouldSelectCandidate) {
                int candidateQueueLength = candidate.getQueLength();
                int bestQueueLength = best.getQueLength();
                boolean candidateHasShorterQueue = candidateQueueLength < bestQueueLength;
                boolean queueLengthsAreEqual = candidateQueueLength == bestQueueLength;

                boolean candidateIsCloserClockwise = false;
                if (queueLengthsAreEqual) {
                    int candidateClockwiseDistance = clockwiseDistance(effectiveZone, candidate.getZone());
                    int bestClockwiseDistance = clockwiseDistance(effectiveZone, best.getZone());
                    candidateIsCloserClockwise = candidateClockwiseDistance < bestClockwiseDistance;
                }

                shouldSelectCandidate = candidateHasShorterQueue
                        || (queueLengthsAreEqual && candidateIsCloserClockwise);
            }

            if (shouldSelectCandidate) {
                best = candidate;
            }
        }


        // When every server is overloaded, return the same-zone server even though its queue is overloaded.
        best = best != null ? best : sameZone;

        if (VERBOSE) {
            // Catches only if it assigns to another zone, which is not the same zone as the request.
            if (best.getZone() != effectiveZone) {
                System.out.println("NOTE: Zone " + zone + " overloaded. ---> Assigning to zone " + best.getZone());
            }       
        }

        return best;
    }

    private ServerInfo findServerInZone(int zone) {
        // Utility function to find the right ServerInfo object given a zone int.
        for (ServerInfo server : serverList) {
            if (server.getZone() == zone) {
                return server;
            }
        }
        return null;
    }

    private int findClockwiseZone(int zone) {
        // If the zone is available, it will just return the same zone, 
        // but if not, it will find the closest zone in a clockwise manner.
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
        // Calculates the clockwise distance between two zones, considering wrap-around.
        // If from fromZone is 5 and toZone is 2, it will return 4, because it goes from 5 -> 1 -> 2.
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
        // Refreshes the queue length of the server asynchronously to avoid blocking the main thread.
        queueRefreshExecutor.submit(() -> {
            ServerAdress address = server.getServerAdress();
            try {
                // Gets the server stub and gets the queue length.
                Registry registry = LocateRegistry.getRegistry(address.getIpAddress(), address.getPort());
                ServerInterface serverStub = (ServerInterface) registry.lookup(address.getServerName());
                int queueLength = serverStub.getQueueLength();

                // This part is synchronized to ensure that the queue length is updated safely in a multi-threaded environment.
                synchronized (this) {
                    server.setQueLength(queueLength);
                    if (VERBOSE) {
                        System.out.println("Zone " + server.getZone() + " queue length: " + queueLength);
                    }
                }
            } catch (RemoteException | NotBoundException | ClassCastException exception) {
                System.err.println("Could not refresh queue length for "
                        + address.getIpAddress() + ": " + exception.getMessage());
            }
        });
    }

}
