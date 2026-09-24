package com.group2.client;

import com.group2.proxy.ServerAdress;
import com.group2.server.ServerInterface;

record RemoteServer(ServerInterface server, ServerAdress address) {
}
