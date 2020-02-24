package ru.kornev.cloudserver.app;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import ru.kornev.cloudcommon.services.LogServiceCommon;
import ru.kornev.cloudcommon.services.settings.Settings;
import ru.kornev.cloudserver.app.handlers.ClientDataHandler;
import ru.kornev.cloudserver.resources.ServerSettings;
import ru.kornev.cloudserver.services.ClientsList;
import ru.kornev.cloudserver.services.LogService;
import ru.kornev.cloudserver.services.console.ConsoleHandler;
import ru.kornev.cloudserver.services.db.DatabaseSQL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class MainServer {

    private DatabaseSQL db;
    private ChannelFuture channelFuture;
    private Path rootDir;
    private ClientsList clientsList;

    public MainServer() {
        try {
            setSettings();
            runDB();
            checkRepositoryExists();
            new ConsoleHandler(this);
            clientsList = new ClientsList(this);
            runServer();
        } catch (IOException | InterruptedException e) {
            LogService.SERVER.error(e);
        }
    }

    public static void main(String[] args) {
        new MainServer();
    }

    private void setSettings() {
        LogServiceCommon.setAppendConsole(true);
        Settings.load("server.cfg", ServerSettings.getSettings());
        rootDir = Paths.get(Settings.get(ServerSettings.ROOT_DIRECTORY));
    }

    private void runDB() {
        db = DatabaseSQL.getInstance();
        db.connect();
    }

    private void checkRepositoryExists() throws IOException {
        if (Files.notExists(rootDir)) Files.createDirectory(rootDir);
    }

    private void runServer() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        int port = Settings.getInt(ServerSettings.SERVER_PORT);
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
            channelFuture = bootstrap.bind(port).sync();
            LogService.SERVER.info("Server starts");
            channelFuture.channel().closeFuture().sync();
        } finally {
            serverShutDown();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public ClientDataHandler addClient(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        return clientsList.addClient(ctx, byteBuf);
    }

    public void deleteClient(ChannelHandlerContext ctx, ClientDataHandler client) {
        clientsList.deleteClient(ctx, client);
    }

    public Path getRootDir() {
        return rootDir;
    }

    public Vector<ClientDataHandler> getClients() {
        return clientsList.getClients();
    }

    public boolean isUserOnline(String login) {
        return clientsList.isUserOnline(login);
    }

    public void serverShutDown() {
        db.shutdown();
        LogService.SERVER.info("Server shutdown");
    }

    public void closeChannel() {
        clientsList.closeAllHandlers();
        channelFuture.channel().close();
    }
}
