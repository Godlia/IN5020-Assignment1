package com.group2.client;

import com.group2.server.ServerInterface;
import com.group2.utils.serverAdress.ServerAdress;

record RemoteServer(ServerInterface server, ServerAdress address) {
}
