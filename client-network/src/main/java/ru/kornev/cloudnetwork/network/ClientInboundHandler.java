package ru.kornev.cloudnetwork.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.kornev.cloudnetwork.resources.NetworkSettings;
import ru.kornev.cloudnetwork.services.LogService;

class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private ByteBuf accumulator;
    private int bufferMinSize;
    private int bufferMaxSize;
    private int bufferSliceIndex;

    private ServerDataHandler serverHandler;

    ClientInboundHandler() {
        bufferMinSize = Network.getSettings().getInt(NetworkSettings.DATA_BUFFER_MIN_SIZE);
        bufferMaxSize = Network.getSettings().getInt(NetworkSettings.DATA_BUFFER_MAX_SIZE);
        bufferSliceIndex = bufferMaxSize / 2;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        accumulator = ByteBufAllocator.DEFAULT.directBuffer(bufferMinSize, bufferMaxSize);
        serverHandler = new ServerDataHandler(ctx, accumulator);
        CallbackHandler.onConnectionEstablished();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        accumulator.release();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        accumulator.writeBytes((ByteBuf) msg);
        serverHandler.handle();
        if (accumulator.readableBytes() == 0) accumulator.clear();
        else if (accumulator.readerIndex() > bufferSliceIndex) accumulator.slice();
        ((ByteBuf) msg).release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogService.SERVER.error(cause.toString());
        cause.printStackTrace();
    }
}
