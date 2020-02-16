package services;

import app.ClientInboundHandler;
import app.Controller;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import settings.GlobalSettings;

import java.net.InetSocketAddress;

public class NetworkThread extends Thread {
    private Controller controller;

    public NetworkThread(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(GlobalSettings.CONNECTION_HOST, GlobalSettings.CONNECTION_PORT))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new ClientInboundHandler(controller));
                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            controller.setLoginDisable(false);
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
