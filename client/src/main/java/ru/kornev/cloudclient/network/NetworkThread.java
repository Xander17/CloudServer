package ru.kornev.cloudclient.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import ru.kornev.cloudclient.app.ClientInboundHandler;
import ru.kornev.cloudclient.resources.ClientSettings;
import ru.kornev.cloudclient.services.GUIForNetworkAdapter;
import ru.kornev.cloudclient.services.LogService;
import ru.kornev.cloudcommon.services.settings.Settings;
import ru.kornev.cloudcommon.settings.GlobalSettings;

import java.net.InetSocketAddress;

public class NetworkThread extends Thread {

    @Override
    public void run() {
        EventLoopGroup group = new NioEventLoopGroup();
        String host = Settings.get(ClientSettings.CONNECTION_HOST);
        int port = Settings.getInt(ClientSettings.CONNECTION_PORT);
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
            GUIForNetworkAdapter.getInstance().afterConnectionInit();
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
