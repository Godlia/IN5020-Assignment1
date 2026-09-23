package com.group2.utils.serverInfo;

import java.io.Serializable;

import com.group2.utils.serverAdress.ServerAdress;

public class ServerInfo implements Serializable {
    private ServerAdress serverAdress;
    private int zone;
    private int queLength;

    public ServerInfo(ServerAdress serverAdress, int zone) {
        this.serverAdress = serverAdress;
        this.zone = zone;
        this.queLength = 0;
    }

    public ServerAdress getServerAdress() {
        return serverAdress;
    }

    public int getZone() {
        return zone;
    }

    public int getQueLength() {
        return queLength;
    }

    public void setQueLength(int queLength) {
        this.queLength = queLength;
    }
}