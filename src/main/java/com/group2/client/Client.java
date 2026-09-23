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
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.group2.proxy.ProxyInterface;
import com.group2.server.ServerInterface;
import com.group2.utils.serverAdress.ServerAdress;

public class Client {
    private static final int DEFAULT_DELAY_MS = 50;
    private static final String DEFAULT_OUTPUT_FILE = "client-output.txt";
    private static final List<String> QUERY_METHODS = List.of(
            "getPopulationofCountry",
            "getNumberofCities",
            "getNumberofCountries",
            "getNumberofCountriesMM");

    public static void main(String[] args) {
        try (Scanner stdinScanner = new Scanner(System.in)) {
            File queryFile = resolveQueryFile(stdinScanner);
            List<QueryRequest> requests = readQueries(queryFile);
            List<QueryResult> results = executeQueries(requests);
            List<String> outputLines = new ArrayList<>();

            for (QueryResult result : results) {
                String line = result.format();
                outputLines.add(line);
                System.out.println(line);
            }
            for (String methodName : QUERY_METHODS) {
                outputLines.add(buildAverageSummary(methodName, results));
            }

            writeOutputFile(outputLines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File resolveQueryFile(Scanner stdinScanner) {
        String filePath = System.getenv("QUERY_FILE");
        if (filePath == null || filePath.isBlank()) {
            System.out.println("Input file path to queryset: ");
            filePath = stdinScanner.nextLine();
        }

        File queryFile = new File(filePath);
        if (!queryFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + filePath);
        }
        return queryFile;
    }

    private static List<QueryRequest> readQueries(File queryFile) throws IOException {
        List<QueryRequest> requests = new ArrayList<>();
        try (Scanner fileScanner = new Scanner(queryFile)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (!line.isBlank()) {
                    requests.add(QueryRequest.parse(line));
                }
            }
        }
        return requests;
    }

    private static List<QueryResult> executeQueries(List<QueryRequest> requests)
            throws InterruptedException, ExecutionException {
        if (requests.isEmpty()) {
            return List.of();
        }

        int poolSize = Math.min(requests.size(), Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
        int delayMs = configuredDelayMs();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(poolSize);
        List<Future<QueryResult>> futures = new ArrayList<>();
        long firstStart = System.nanoTime();

        try {
            for (int index = 0; index < requests.size(); index++) {
                QueryRequest request = requests.get(index);
                long scheduledStart = firstStart + TimeUnit.MILLISECONDS.toNanos((long) index * delayMs);
                long delay = Math.max(0, scheduledStart - System.nanoTime());
                futures.add(executor.schedule(
                    () -> executeQuery(request, scheduledStart),
                        delay,
                        TimeUnit.NANOSECONDS));
            }

            List<QueryResult> results = new ArrayList<>();
            for (Future<QueryResult> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

        private static QueryResult executeQuery(QueryRequest request, long scheduledStart)
            throws RemoteException, NotBoundException {
        RemoteServer remoteServer = connectToServer(request.zone());
        long executionStart = System.nanoTime();
        Object result = invokeServer(remoteServer.server(), request);
        long finished = System.nanoTime();

        long turnaroundMs = nanosToMillis(finished - scheduledStart);
        long executionMs = nanosToMillis(finished - executionStart);
        long waitingMs = Math.max(0, turnaroundMs - executionMs);
        return new QueryResult(request, result, remoteServer.address(),
                turnaroundMs, executionMs, waitingMs);
    }

    private static int configuredDelayMs() {
        String value = System.getenv("CLIENT_DELAY_MS");
        return value == null || value.isBlank() ? DEFAULT_DELAY_MS : Integer.parseInt(value);
    }

    private static long nanosToMillis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    private static Object invokeServer(ServerInterface server, QueryRequest request) throws RemoteException {
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

    private static String buildAverageSummary(String methodName, List<QueryResult> results) {
        long count = 0;
        long turnaroundTotal = 0;
        long executionTotal = 0;
        long waitingTotal = 0;
        long minTurnaround = Long.MAX_VALUE;
        long maxTurnaround = Long.MIN_VALUE;

        for (QueryResult result : results) {
            if (!result.request().methodName().equals(methodName)) {
                continue;
            }
            count++;
            turnaroundTotal += result.turnaroundMs();
            executionTotal += result.executionMs();
            waitingTotal += result.waitingMs();
            minTurnaround = Math.min(minTurnaround, result.turnaroundMs());
            maxTurnaround = Math.max(maxTurnaround, result.turnaroundMs());
        }

        if (count == 0) {
            return methodName + " avg turn-around time: 0 ms, avg execution time: 0 ms, avg waiting time: 0 ms, min turn-around time: 0 ms, max turn-around time: 0 ms";
        }
        return methodName + " avg turn-around time: " + turnaroundTotal / count
                + " ms, avg execution time: " + executionTotal / count
                + " ms, avg waiting time: " + waitingTotal / count
                + " ms, min turn-around time: " + minTurnaround
                + " ms, max turn-around time: " + maxTurnaround + " ms";
    }

    private static void writeOutputFile(List<String> outputLines) throws IOException {
        String outputFile = System.getenv().getOrDefault("OUTPUT_FILE", DEFAULT_OUTPUT_FILE);
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            for (String line : outputLines) {
                writer.println(line);
            }
        }
    }

    private static RemoteServer connectToServer(int zone) throws RemoteException, NotBoundException {
        ServerAdress proxyAdress = new ServerAdress("proxy", 1099, "proxy");
        ProxyInterface proxy = (ProxyInterface) getStub(proxyAdress);
        ServerAdress serverAdress = proxy.RequestServer(zone);
        if (serverAdress == null) {
            throw new IllegalStateException("No server available for zone " + zone);
        }
        return new RemoteServer((ServerInterface) getStub(serverAdress), serverAdress);
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

    private record RemoteServer(ServerInterface server, ServerAdress address) {
    }

    private record QueryResult(QueryRequest request, Object result, ServerAdress serverAddress,
            long turnaroundMs, long executionMs, long waitingMs) {
        private String format() {
            return result + " " + request.originalQuery() + " (turnaround time: " + turnaroundMs
                    + " ms, execution time: " + executionMs + " ms, waiting time: " + waitingMs
                    + " ms, processed by Server " + serverNumber(serverAddress) + ")";
        }

        private static String serverNumber(ServerAdress address) {
            String name = address.getServerName();
            int separator = name.lastIndexOf("server-zone");
            return separator >= 0 ? name.substring(separator + "server-zone".length()) : name;
        }
    }

}