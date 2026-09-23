package com.group2.client;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.group2.proxy.ProxyInterface;
import com.group2.server.ServerInterface;
import com.group2.utils.serverAdress.ServerAdress;

public class Client {
    private static final int T = 50;
    private static final String OUTPUT_FILE = "client-output.txt";

    public static void main(String[] args) {
        try (Scanner stdinScanner = new Scanner(System.in)) {
            String envFile = System.getenv("QUERY_FILE");
            String filePath = envFile != null && !envFile.isBlank() ? envFile : null;

            if (filePath == null) {
                System.out.println("Input file path to queryset: ");
                filePath = stdinScanner.nextLine();
            }

            File queryFile = new File(filePath);
            if (!queryFile.exists()) {
                throw new IllegalArgumentException("Input file does not exist: " + filePath);
            }

            List<String> outputLines = new ArrayList<>();
            Map<String, List<Long>> methodStats = new HashMap<>();
            methodStats.put("getPopulationofCountry", new ArrayList<>());
            methodStats.put("getNumberofCities", new ArrayList<>());
            methodStats.put("getNumberofCountries", new ArrayList<>());
            methodStats.put("getNumberofCountriesMM", new ArrayList<>());

            try (Scanner fileScanner = new Scanner(queryFile)) {
                while (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine().trim();
                    if (line.isBlank()) {
                        continue;
                    }

                    long requestStart = System.currentTimeMillis();
                    QueryRequest request = QueryRequest.parse(line);
                    int zone = request.zone();
                    ServerInterface server = connectToServer(zone);

                    long executionStart = System.currentTimeMillis();
                    Object result = invokeServer(server, request);
                    long executionTime = System.currentTimeMillis() - executionStart;
                    long turnaroundTime = System.currentTimeMillis() - requestStart;
                    long waitingTime = turnaroundTime - executionTime;

                    String summary = result + " " + request.originalQuery() + " (turnaround time: "
                            + turnaroundTime + " ms, execution time: " + executionTime + " ms, waiting time: "
                            + waitingTime + " ms, processed by Server " + zone + ")";
                    outputLines.add(summary);
                    System.out.println(summary);

                    recordMethodStats(methodStats, request.methodName(), turnaroundTime, executionTime, waitingTime);

                    try {
                        Thread.sleep(T);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            for (String methodName : List.of("getPopulationofCountry", "getNumberofCities", "getNumberofCountries", "getNumberofCountriesMM")) {
                outputLines.add(buildAverageSummary(methodName, methodStats.get(methodName)));
            }

            writeOutputFile(outputLines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object invokeServer(ServerInterface server, QueryRequest request) throws Exception {
        return switch (request.methodName()) {
            case "getPopulationofCountry" -> server.getPopulationofCountry((String) request.arguments()[0]);
            case "getNumberofCities" -> server.getNumberofCities((String) request.arguments()[0],
                    (int) request.arguments()[1], (String) request.arguments()[2]);
            case "getNumberofCountries" -> server.getNumberofCountries((int) request.arguments()[0],
                    (int) request.arguments()[1], (String) request.arguments()[2]);
            case "getNumberofCountriesMM" -> server.getNumberofCountriesMM((int) request.arguments()[0],
                    (int) request.arguments()[1], (int) request.arguments()[2]);
            default -> throw new IllegalArgumentException("Unknown method: " + request.methodName());
        };
    }

    private static void recordMethodStats(Map<String, List<Long>> methodStats, String methodName, long turnaroundTime, long executionTime, long waitingTime) {
        List<Long> stats = methodStats.get(methodName);
        if (stats == null) {
            stats = new ArrayList<>();
            methodStats.put(methodName, stats);
        }
        stats.add(turnaroundTime);
        stats.add(executionTime);
        stats.add(waitingTime);
    }

    private static String buildAverageSummary(String methodName, List<Long> stats) {
        if (stats == null || stats.isEmpty()) {
            return methodName + " avg turn-around time: 0 ms, avg execution time: 0 ms, avg waiting time: 0 ms, min turn-around time: 0 ms, max turn-around time: 0 ms";
        }

        int entries = stats.size() / 3;
        long turnaroundTotal = 0;
        long executionTotal = 0;
        long waitingTotal = 0;
        long minTurnaround = Long.MAX_VALUE;
        long maxTurnaround = Long.MIN_VALUE;

        for (int i = 0; i < stats.size(); i += 3) {
            long turnaround = stats.get(i);
            long execution = stats.get(i + 1);
            long waiting = stats.get(i + 2);
            turnaroundTotal += turnaround;
            executionTotal += execution;
            waitingTotal += waiting;
            minTurnaround = Math.min(minTurnaround, turnaround);
            maxTurnaround = Math.max(maxTurnaround, turnaround);
        }

        return methodName + " avg turn-around time: " + (turnaroundTotal / entries)
                + " ms, avg execution time: " + (executionTotal / entries)
                + " ms, avg waiting time: " + (waitingTotal / entries)
                + " ms, min turn-around time: " + minTurnaround
                + " ms, max turn-around time: " + maxTurnaround + " ms";
    }

    private static void writeOutputFile(List<String> outputLines) throws IOException {
        try (PrintWriter writer = new PrintWriter(OUTPUT_FILE)) {
            for (String line : outputLines) {
                writer.println(line);
            }
        }
    }

    private static ServerInterface connectToServer(int zone) throws RemoteException, NotBoundException {
        ServerAdress proxyAdress = new ServerAdress("proxy", 1099, "proxy");
        ProxyInterface proxy = (ProxyInterface) getStub(proxyAdress);
        ServerAdress serverAdress = proxy.RequestServer(zone);
        if (serverAdress == null) {
            throw new IllegalStateException("No server available for zone " + zone);
        }
        return (ServerInterface) getStub(serverAdress);
    }

    private static Remote getStub(ServerAdress serveradress) throws RemoteException, NotBoundException {
        try {
            Registry registry = LocateRegistry.getRegistry(serveradress.getIpAddress(), serveradress.getPort());
            return registry.lookup(serveradress.getServerName());
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Failed to connect to server: " + serveradress.getServerName() + " at " + serveradress.getIpAddress() + ":" + serveradress.getPort());
            throw e;
        }
    }

    private record QueryRequest(String originalQuery, String methodName, Object[] arguments, int zone) {
        private static QueryRequest parse(String line) {
            String[] tokens = line.split("\\s+");
            int zoneIndex = -1;
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].startsWith("Zone:")) {
                    zoneIndex = i;
                    break;
                }
            }

            if (zoneIndex < 0) {
                throw new IllegalArgumentException("Missing Zone value in query: " + line);
            }

            String methodName = tokens[0];
            String[] argumentTokens = Arrays.copyOfRange(tokens, 1, zoneIndex);
            Object[] arguments = buildArguments(methodName, argumentTokens);
            int zone = Integer.parseInt(tokens[zoneIndex].substring("Zone:".length()));
            return new QueryRequest(line, methodName, arguments, zone);
        }

        private static Object[] buildArguments(String methodName, String[] tokens) {
            return switch (methodName) {
                case "getPopulationofCountry" -> new Object[] { String.join(" ", tokens) };
                case "getNumberofCities" -> new Object[] {
                        String.join(" ", Arrays.copyOfRange(tokens, 0, tokens.length - 2)),
                        Integer.parseInt(tokens[tokens.length - 2]),
                        tokens[tokens.length - 1]
                };
                case "getNumberofCountries" -> new Object[] {
                        Integer.parseInt(tokens[0]),
                        Integer.parseInt(tokens[1]),
                        tokens[2]
                };
                case "getNumberofCountriesMM" -> new Object[] {
                        Integer.parseInt(tokens[0]),
                        Integer.parseInt(tokens[1]),
                        Integer.parseInt(tokens[2])
                };
                default -> throw new IllegalArgumentException("Unknown method: " + methodName);
            };
        }
    }
}