package com.group2.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class ServerRequestQueue {
    private static final int NETWORK_LATENCY_MS = 80;
    private static final int INTER_ZONE_LATENCY_MS = 30;

    private final LinkedBlockingQueue<FutureTask<?>> waitingTasks = new LinkedBlockingQueue<>();
    private final PrintWriter queueLog;
    private volatile int serverZone;

    ServerRequestQueue() {
        this.queueLog = openLog();
        Thread worker = new Thread(this::runTasks, "server-request-worker");
        worker.start();
    }

    <T> T submit(Callable<T> request) throws Exception {
        return submit(serverZone, request);
    }

    <T> T submit(int requestedZone, Callable<T> request) throws Exception {
        logQueueSize("received");
        try {
            int zoneDistance = Math.abs(requestedZone - serverZone);
            long latencyMs = NETWORK_LATENCY_MS + (long) zoneDistance * INTER_ZONE_LATENCY_MS;
            //System.out.println("Was requested zone :" + requestedZone + ". We are: " + serverZone + ". Latency given = " + latencyMs + ". ");
            TimeUnit.MILLISECONDS.sleep(latencyMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Interrupted before request was queued");
        }

        FutureTask<T> task = new FutureTask<>(request);
        waitingTasks.put(task);
        logQueueSize("enqueued");
        try {
            return task.get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checkedException) {
                throw checkedException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Server request failed", cause);
        }
    }

    int size() {
        return waitingTasks.size();
    }

    void setServerZone(int serverZone) {
        this.serverZone = serverZone;
    }

    private void runTasks() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                FutureTask<?> task = waitingTasks.take();
                logQueueSize("dequeued");
                task.run();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void logQueueSize(String event) {
        queueLog.println(Instant.now().toEpochMilli() + " " + event + " queue-size=" + waitingTasks.size());
        queueLog.flush();
    }

    private static PrintWriter openLog() {
        String defaultDirectory = System.getenv().getOrDefault("OUTPUT_DIR", runOutputDirectory());
        String logFile = System.getenv().getOrDefault("QUEUE_LOG_FILE",
                Path.of(defaultDirectory, "server-queue.log").toString());
        try {
            Path path = Path.of(logFile);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return new PrintWriter(path.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not open queue log " + logFile, exception);
        }
    }

    private static String runOutputDirectory() {
        String cacheType = System.getenv().getOrDefault("CACHE_TYPE", "NAIVE");
        String delayMs = System.getenv().getOrDefault("CLIENT_DELAY_MS", "20");
        return Path.of("output", cacheType + delayMs).toString();
    }
}