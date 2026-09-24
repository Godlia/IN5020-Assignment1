package com.group2.client;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.group2.cache.Cache;
import com.group2.cache.CacheKey;
import com.group2.cache.CacheType;
import com.group2.cache.EvictionPolicy;
import com.group2.proxy.ProxyInterface;
import com.group2.proxy.ServerAdress;
import com.group2.server.ServerInterface;

public class Client {

    //determine cachemode and instantiate it
    private static final String CACHE_MODE = System.getenv("CACHE_MODE");
    private static final boolean CACHE_ENABLED = CACHE_MODE != null
            && ("LRU".equalsIgnoreCase(CACHE_MODE)
            || "FIFO".equalsIgnoreCase(CACHE_MODE));

    private static final Cache<CacheKey, Object> CACHE = Cache.create(
            CacheType.CLIENT,
            EvictionPolicy.valueOf(
                    CACHE_ENABLED ? CACHE_MODE.toUpperCase() : "LRU"
            )
    );

    // Fallback arguments
    private static final int DEFAULT_DELAY_MS = 20;
    private static final String DEFAULT_OUTPUT_FILE = "client-output.txt";
    private static final List<String> QUERY_METHODS = List.of(
            "getPopulationofCountry",
            "getNumberofCities",
            "getNumberofCountries",
            "getNumberofCountriesMM");

    public static void main(String[] args) {
        //Read the exercise input file and convert the method & args into a query object
        try (Scanner stdinScanner = new Scanner(System.in)) {
            //Start total timer
            long totalStart = System.nanoTime();
            File queryFile = resolveQueryFile(stdinScanner);
            List<QueryRequest> requests = readQueries(queryFile); //create list of queries
            List<QueryResult> results = executeQueries(requests); //execute queries
            List<String> outputLines = new ArrayList<>();

            for (QueryResult result : results) {
                String line = result.format();
                outputLines.add(line);
            }
            //averages for methods
            for (String methodName : QUERY_METHODS) {
                outputLines.add(buildAverageSummary(methodName, results));
            }

            long totalElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - totalStart);
            String totalLine = "Total elapsed time: " + totalElapsed + " ms";
            outputLines.add(totalLine);
            System.out.println(totalLine);

            writeOutputFile(outputLines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //checks if inputfile is set, otherwise assume stdin
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

    //convert the lines from the queryfile into a queryRequest object
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

    //wrapper around executeQueries() for executing the whole list 
    private static List<QueryResult> executeQueries(List<QueryRequest> requests)
            throws InterruptedException, ExecutionException {
        if (requests.isEmpty()) {
            return List.of();
        }

        int delayMs = configuredDelayMs(); //get the set millisecond delay : [50, 20]
        //start independent thread for async method invocation, and its components
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<QueryResult>> futures = new ArrayList<>();
        long firstStart = System.nanoTime();

        //fetches all the requests and starts the thread to await response from executeQuery()
        try {
            for (int index = 0; index < requests.size(); index++) {
                QueryRequest request = requests.get(index);

                long scheduledStart = firstStart + TimeUnit.MILLISECONDS.toNanos((long) index * delayMs);
                futures.add(executor.submit(() -> {
                    long delay = scheduledStart - System.nanoTime();
                    if (delay > 0) {
                        TimeUnit.NANOSECONDS.sleep(delay);
                    }
                    return executeQuery(request, scheduledStart);
                }));
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

    /*
    Executes to the remote server. 

    Checks the cache for a hit, otherwise connects to the proxy for a server-stub,
    then executes.
    */    private static QueryResult executeQuery(QueryRequest request, long scheduledStart)
            throws RemoteException, NotBoundException, InterruptedException {

        long executionStart = System.nanoTime();
        CacheKey key = CacheKey.of(request.methodName(), request.arguments());
        if (CACHE_ENABLED) {
            CachedResponse cachedResponse = (CachedResponse) CACHE.get(key);
            if (cachedResponse != null) {
                System.out.println("Cache hit for query: " + request.originalQuery());
                QueryResult queryResult = timedResult(request, cachedResponse.result(),
                        cachedResponse.serverAddress(), scheduledStart, executionStart);
                printInvocationResult(queryResult);
                return queryResult;
            }
        }

        RemoteServer remoteServer = connectToServer(request.zone());
        Object result = invokeServer(remoteServer.server(), request); //invoke to the server
        QueryResult queryResult = timedResult(request, result, remoteServer.address(),
                scheduledStart, executionStart);
        printInvocationResult(queryResult);

        if (CACHE_ENABLED) {
            CACHE.put(key, new CachedResponse(result, remoteServer.address()));
        }

        return queryResult;
    }

    private static void printInvocationResult(QueryResult queryResult) {
        System.out.println(queryResult.format());
    }

    //get the result with the amassed time taken
    private static QueryResult timedResult(QueryRequest request, Object result,
            ServerAdress serverAddress, long scheduledStart, long executionStart) {
        long finished = System.nanoTime();
        long turnaroundMs = nanosToMillis(finished - scheduledStart);
        long executionMs = nanosToMillis(finished - executionStart);
        long waitingMs = Math.max(0, turnaroundMs - executionMs);
        return new QueryResult(request, result, serverAddress, turnaroundMs, executionMs, waitingMs);
    }

    //record for a cachedResponse
    private record CachedResponse(Object result, ServerAdress serverAddress) {

    }
    //fetches environment for the clients delay time
    private static int configuredDelayMs() {
        String value = System.getenv("CLIENT_DELAY_MS");
        return value == null || value.isBlank() ? DEFAULT_DELAY_MS : Integer.parseInt(value);
    }

    
    private static long nanosToMillis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    /*
    Gets a serverstub and a request, then calls the corresponding method on the serverinterface
    */
    private static Object invokeServer(ServerInterface server, QueryRequest request) throws RemoteException {
        return switch (request.methodName()) {
            case "getPopulationofCountry" ->
                server.getPopulationofCountry(
                (String) request.arguments()[0], request.zone());
            case "getNumberofCities" ->
                server.getNumberofCities((String) request.arguments()[0],
                (int) request.arguments()[1], (String) request.arguments()[2], request.zone());
            case "getNumberofCountries" ->
                server.getNumberofCountries((int) request.arguments()[0],
                (int) request.arguments()[1], (String) request.arguments()[2], request.zone());
            case "getNumberofCountriesMM" ->
                server.getNumberofCountriesMM((int) request.arguments()[0],
                (int) request.arguments()[1], (int) request.arguments()[2], request.zone());
            default ->
                throw new IllegalArgumentException("Unknown method: " + request.methodName());
        };
    }

    //for the last lines of the log
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

    //gets the output directory from env, and writes out
    private static void writeOutputFile(List<String> outputLines) throws IOException {
        Path outputFile = Path.of(System.getenv().getOrDefault(
                "OUTPUT_FILE", runOutputDirectory().resolve(DEFAULT_OUTPUT_FILE).toString()));
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (PrintWriter writer = new PrintWriter(outputFile.toFile())) {
            for (String line : outputLines) {
                writer.println(line);
            }
        }
    }


    private static Path runOutputDirectory() {
        String configuredDirectory = System.getenv("OUTPUT_DIR");
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            return Path.of(configuredDirectory);
        }

        String cacheType = System.getenv().getOrDefault("CACHE_TYPE", "NAIVE");
        String delayMs = System.getenv().getOrDefault("CLIENT_DELAY_MS", "20");
        return Path.of("output", cacheType + delayMs);
    }

    //connects to the proxy and fetches the server interface provided to the client
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

}
