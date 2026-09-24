package com.group2.proxy;

import java.io.Serializable;

public class ServerAdress implements Serializable {
    private String ipAddress;
    private int port;
    private String serverName;
    private int serverNumber;

    public ServerAdress(String ipAddress, int port, String serverName) {
        this(ipAddress, port, serverName, 0);
    }

    public ServerAdress(String ipAddress, int port, String serverName, int serverNumber) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.serverName = serverName;
        this.serverNumber = serverNumber;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getServerName() {
        return serverName;
    }

    public int getServerNumber() {
        return serverNumber;
    }
}