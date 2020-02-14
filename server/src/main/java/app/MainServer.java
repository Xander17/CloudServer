package app;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import services.ConsoleHandler;
import services.DatabaseSQL;
import services.LogService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class MainServer {

    public static final Path REPOSITORY_ROOT = Paths.get("server-repo");
    private DatabaseSQL db;
    private ChannelFuture channelFuture;

    private Vector<ClientHandler> clients = new Vector<>();

    public MainServer() {
        try {
            runDB();
            checkRepositoryExists();
            new ConsoleHandler(this);
            runServer();
        } catch (IOException e) {
            LogService.SERVER.error(e.toString());
        } catch (InterruptedException e) {
            LogService.SERVER.error(e.toString());
        }
    }

    private void runDB() {
        db = DatabaseSQL.getInstance();
        db.connect();
    }

    private void checkRepositoryExists() throws IOException {
        if (Files.notExists(REPOSITORY_ROOT)) Files.createDirectory(REPOSITORY_ROOT);
    }

    private void runServer() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ServerInboundHandler(MainServer.this));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            channelFuture = bootstrap.bind(8189).sync();
            LogService.SERVER.info("Server starts");
            channelFuture.channel().closeFuture().sync();
        } finally {
            serverShutDown();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public ClientHandler addClient(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        ClientHandler clientHandler = new ClientHandler(this, ctx, byteBuf);
        clients.add(clientHandler);
        LogService.SERVER.info("New client connected", ctx.channel().remoteAddress().toString(), getConnectionsCountInfo());
        return clientHandler;
    }

    public void deleteClient(ChannelHandlerContext ctx, ClientHandler client) {
        clients.remove(client);
        LogService.SERVER.info("Client disconnected", client.getLogin(), ctx.channel().remoteAddress().toString(), getConnectionsCountInfo());
    }

    public boolean isUserOnline(String login) {
        for (ClientHandler client : clients) {
            if (client.getLogin() == null) continue;
            if (client.getLogin().equals(login)) return true;
        }
        return false;
    }

    private String getConnectionsCountInfo() {
        return "Total connected clients: " + clients.size();
    }

    public void serverShutDown() {
        db.shutdown();
        LogService.SERVER.info("Server shutdown");
    }

    public void closeChannel() {
        clients.forEach(ClientHandler::closeChannel);
        channelFuture.channel().close();
    }

    public Vector<ClientHandler> getClients() {
        return clients;
    }

    public static void main(String[] args) {
        new MainServer();
    }
}
