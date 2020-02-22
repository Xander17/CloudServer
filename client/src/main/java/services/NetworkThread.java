package services;

import app.ClientInboundHandler;
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
                            socketChannel.pipeline().addLast(new ClientInboundHandler());
                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            GUIForNetworkAdapter.getInstance().setConnectionEstablishedState();
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
