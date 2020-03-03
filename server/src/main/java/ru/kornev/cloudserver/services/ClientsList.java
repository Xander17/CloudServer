package ru.kornev.cloudserver.services;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudserver.app.MainServer;
import ru.kornev.cloudserver.app.handlers.ClientDataHandler;

import java.util.Vector;

public class ClientsList {

    private Vector<ClientDataHandler> clients = new Vector<>();
    private MainServer server;

    public ClientsList(MainServer server) {
        this.server = server;
    }

    public ClientDataHandler addClient(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        ClientDataHandler clientHandler = new ClientDataHandler(server, ctx, byteBuf);
        clients.add(clientHandler);
        LogService.SERVER.info("New client connected", ctx.channel().remoteAddress().toString(), getConnectionsCountInfo());
        return clientHandler;
    }

    public void deleteClient(ChannelHandlerContext ctx, ClientDataHandler client) {
        clients.remove(client);
        LogService.SERVER.info("Client disconnected", client.getLogin(), ctx.channel().remoteAddress().toString(), getConnectionsCountInfo());
    }

    public boolean isUserOnline(String login) {
        for (ClientDataHandler client : clients) {
            if (client.getLogin() == null) continue;
            if (client.getLogin().equals(login)) return true;
        }
        return false;
    }

    private String getConnectionsCountInfo() {
        return "Total connected clients: " + clients.size();
    }

    public Vector<ClientDataHandler> getClients() {
        return clients;
    }

    public void closeAllHandlers() {
        clients.forEach(ClientDataHandler::closeChannel);
    }
}
