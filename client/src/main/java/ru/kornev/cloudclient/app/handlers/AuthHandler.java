package ru.kornev.cloudclient.app.handlers;

import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudclient.services.GUIForNetworkAdapter;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.services.transfer.CommandPackage;
import ru.kornev.cloudcommon.services.transfer.DataSocketWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AuthHandler {

    private ServerDataHandler dataHandler;
    private ChannelHandlerContext ctx;

    AuthHandler(ServerDataHandler dataHandler, ChannelHandlerContext ctx) {
        this.dataHandler = dataHandler;
        this.ctx = ctx;
    }

    void setRegSuccess() {
        GUIForNetworkAdapter.getInstance().setRegistrationSuccess();
    }

    void setAuthSuccess() throws IOException {
        dataHandler.authSuccess();
        GUIForNetworkAdapter.getInstance().setAuthorizationSuccess();
    }

    void setRegAuthError(CommandPackage commandPackage) {
        GUIForNetworkAdapter.getInstance().setRegAuthError(commandPackage.getIntCommandData());
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
        DataSocketWriter.sendCommand(ctx, command, (byte) (loginBytes.length), (byte) (passBytes.length));
        DataSocketWriter.sendData(ctx, loginBytes, passBytes);
    }
}
