package com.group2.utils.serverAdress;

import java.io.Serializable;

public class ServerAdress implements Serializable {
    private String ipAddress;
    private int port;
    private String serverName;

    public ServerAdress(String ipAddress, int port, String serverName) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.serverName = serverName;
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
}