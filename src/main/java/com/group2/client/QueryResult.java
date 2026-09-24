package com.group2.client;

import com.group2.proxy.ServerAdress;

record QueryResult(QueryRequest request, Object result, ServerAdress serverAddress,
        long turnaroundMs, long executionMs, long waitingMs) {
    String format() {
        return result + " " + request.originalQuery() + " (turnaround time: " + turnaroundMs
                + " ms, execution time: " + executionMs + " ms, waiting time: " + waitingMs
                + " ms, processed by Server " + serverNumber(serverAddress) + ")";
    }

    private static String serverNumber(ServerAdress address) {
        if (address.getServerNumber() > 0) {
            return Integer.toString(address.getServerNumber());
        }
        String name = address.getServerName();
        int separator = name.lastIndexOf("server-zone");
        return separator >= 0 ? name.substring(separator + "server-zone".length()) : name;
    }
}
