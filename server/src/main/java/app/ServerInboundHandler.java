package app;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import resources.ServerSettings;
import services.LogService;
import services.settings.Settings;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

    private MainServer server;
    private ClientHandler clientHandler;
    private ByteBuf accumulator;
    private int bufferMinSize;
    private int bufferMaxSize;
    private int bufferSliceIndex;

    public ServerInboundHandler(MainServer server) {
        this.server = server;
        bufferMinSize = Settings.getInt(ServerSettings.INBOUND_BUFFER_MIN_SIZE);
        bufferMaxSize = Settings.getInt(ServerSettings.INBOUND_BUFFER_MAX_SIZE);
        bufferSliceIndex = bufferMaxSize / 2;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        accumulator = ByteBufAllocator.DEFAULT.directBuffer(bufferMinSize, bufferMaxSize);
        clientHandler = server.addClient(ctx, accumulator);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        accumulator.release();
        server.deleteClient(ctx, clientHandler);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        accumulator.writeBytes((ByteBuf) msg);
        clientHandler.handle();
        if (accumulator.readableBytes() == 0) accumulator.clear();
            // TODO: 14.02.2020 проверить работу слайса
        else if (accumulator.writerIndex() > bufferSliceIndex) accumulator.slice();
        ((ByteBuf) msg).release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogService.SERVER.error(cause.toString());
        cause.printStackTrace();
    }
}
