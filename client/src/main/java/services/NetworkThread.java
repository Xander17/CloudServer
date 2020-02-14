package services;

import app.ClientInboundHandler;
import app.Controller;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import settings.GlobalSettings;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class NetworkThread extends Thread {
    private CountDownLatch countDownLatch;
    private Controller controller;
    private Channel channel;

    private ClientInboundHandler clientInboundHandler;

    public NetworkThread(Controller controller, CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
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
                            clientInboundHandler = new ClientInboundHandler(controller);
                            socketChannel.pipeline().addLast(clientInboundHandler);
                            channel = socketChannel;
                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            countDownLatch.countDown();
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

    public Channel getChannel() {
        return channel;
    }

    public ClientInboundHandler getClientInboundHandler() {
        return clientInboundHandler;
    }
}
