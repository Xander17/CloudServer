package ru.kornev.cloudnetwork.network;

import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.services.transfer.CommandSender;

import java.nio.charset.StandardCharsets;

class AuthHandler {

    private ChannelHandlerContext ctx;

    AuthHandler(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    void signIn(String login, String pass) {
        sendRegAuthData(CommandBytes.AUTH, login, pass);
    }

    void signUp(String login, String pass) {
        sendRegAuthData(CommandBytes.REG, login, pass);
    }

    private void sendRegAuthData(CommandBytes command, String login, String pass) {
        if (!CommandBytes.REG.equals(command) && !CommandBytes.AUTH.equals(command)) return;
        byte[] loginBytes = login.getBytes(StandardCharsets.UTF_8);
        byte[] passBytes = pass.getBytes(StandardCharsets.UTF_8);
        CommandSender.sendCommand(ctx, command, (byte) (loginBytes.length), (byte) (passBytes.length));
        CommandSender.sendData(ctx, loginBytes, passBytes);
    }
}
