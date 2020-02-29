package ru.kornev.cloudnetwork.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import ru.kornev.cloudcommon.services.settings.Settings;
import ru.kornev.cloudnetwork.resources.NetworkSettings;
import ru.kornev.cloudnetwork.services.LogService;

import java.net.InetSocketAddress;

public class Network extends Thread {

    private static Settings settings;

    public static Settings getSettings() {
        return settings;
    }

    @Override
    public void run() {
        settings = new Settings("network.cfg", NetworkSettings.getSettings());
        EventLoopGroup group = new NioEventLoopGroup();
        String host = settings.get(NetworkSettings.CONNECTION_HOST);
        int port = settings.getInt(NetworkSettings.CONNECTION_PORT);
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new ClientInboundHandler());
                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            LogService.SERVER.error(e);
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                LogService.SERVER.error(e);
            }
        }
    }
}
