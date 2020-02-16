package app;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import services.LogService;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {
    private int BUFFER_MIN_SIZE = 100 * 1024;
    private int BUFFER_MAX_SIZE = 1024 * 1024 * 5;
    private int BUFFER_SLICE_INDEX = 1024 * 1024;

    private Controller controller;
    private ByteBuf accumulator;

    private DataHandler serverHandler;

    public ClientInboundHandler(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        accumulator = ByteBufAllocator.DEFAULT.directBuffer(BUFFER_MIN_SIZE, BUFFER_MAX_SIZE);
        serverHandler = new DataHandler(ctx, accumulator, controller);
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
            // TODO: 14.02.2020 проверить работу слайса
        else if (accumulator.readerIndex() > BUFFER_SLICE_INDEX) accumulator.slice();
        ((ByteBuf) msg).release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogService.SERVER.error(cause.toString());
        cause.printStackTrace();
    }
}
