package app;

import resources.ControlMessage;
import resources.LoginRegError;
import services.AuthService;
import services.LogService;
import settings.GlobalSettings;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ClientHandler {

    private MainServer server;
    private Socket socket;
    private String login;
    private Integer id;
    private Path rootDir;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientHandler(MainServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        startClientThread();
    }

    private void startClientThread() {
        new Thread(() -> {
            try {
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                getUserLoginReg();
                startDataListener();
            } catch (IOException e) {
                LogService.SERVER.error(socket.toString(), e.toString());
            } finally {
                LogService.USERS.info("Disconnect", login, socket.toString());
                closeIOStreams();
                server.deleteClient(this);
            }
        }).start();
    }

    private void getUserLoginReg() throws IOException {
        String[] loginPassPair;
        String inputStr;
        while (true) {
            if(!checkCommandStart(in.read())) continue;
            inputStr = in.readUTF();
            loginPassPair = inputStr.split(" ", 3);
            if (loginPassPair.length != 3) continue;
            if (ControlMessage.AUTH.check(loginPassPair[0])) {
                LogService.AUTH.info(socket.toString(), "Auth attempt");
                id = AuthService.checkLogin(loginPassPair[1], loginPassPair[2]);
                if (id == null) sendLoginRegError(LoginRegError.INCORRECT_LOGIN_PASS);
                //else if (server.isUserOnline(login)) sendLoginRegError(LoginRegError.LOGGED_ALREADY);
                    // TODO: 09.02.2020 подумать, нужно ли запрещать подключаться под одним логином
                else {
                    login = loginPassPair[1];
                    sendCommand(ControlMessage.AUTH_OK, id.toString());

                    // TODO: 09.02.2020 отправить список файлов

                    LogService.AUTH.info(login, socket.toString(), "Auth success");
                    break;
                }
            } else if (ControlMessage.REG.check(loginPassPair[0])) {
                LogService.AUTH.info(socket.toString(), "Registration attempt");
                String login = loginPassPair[1];
                String pass = loginPassPair[2];
                LoginRegError error = AuthService.registerAndEchoMsg(login, pass);
                if (error == null) {
                    sendCommand(ControlMessage.REG_OK);
                    LogService.AUTH.info(login, socket.toString(), "Registration success");
                } else sendLoginRegError(error);
            }
        }
    }

    private void startDataListener() {
        try {
            rootDir = MainServer.REPOSITORY_ROOT.resolve(login);
            if (Files.notExists(rootDir)) Files.createDirectory(rootDir);
            int byteRead;
            while (true) {
                byteRead = in.read();
                if (checkPackageStart(byteRead)) downloadFile();
                else if (checkCommandStart(byteRead)) ;
                // TODO: 09.02.2020 обработать команды от клиента
            }
        } catch (IOException e) {
            LogService.USERS.error("IO", e.toString());
        } catch (NoSuchAlgorithmException e) {
            LogService.SERVER.error(login, "Checksum algorithm error", e.toString());
            LogService.USERS.error(login, "Checksum algorithm error", e.toString());
        }
    }

    private boolean checkPackageStart(int b) {
        return b == GlobalSettings.PACKAGE_START_SIGNAL_BYTE;
    }

    private boolean checkCommandStart(int b) {
        return b == GlobalSettings.COMMAND_START_SIGNAL_BYTE;
    }

    private boolean downloadFile() throws IOException, NoSuchAlgorithmException {
        LogService.USERS.info(login, "Package start checked");
        String name = getFileName();
        if (name == null) {
            LogService.USERS.info(login, "Filename reading failed");
            return false;
        }
        LogService.USERS.info(login, "Downloading", name);
        if (!downloadFileData(name)) {
            LogService.USERS.info(login, "Download failed", name);
            return false;
        }
        LogService.USERS.info(login, "Download complete", name);
        return true;
    }

    private String getFileName() throws IOException {
        int length = in.readShort();
        byte[] bytes = new byte[length];
        if (in.read(bytes) != length) return null;
        return new String(bytes);
    }

    private boolean downloadFileData(String filename) throws IOException, NoSuchAlgorithmException {
        long length = in.readLong();
        if (length <= 0) return false;
        byte[] bytes = new byte[MainServer.BUFFER_SIZE];
        MessageDigest md = MessageDigest.getInstance(GlobalSettings.CHECKSUM_PROTOCOL);
        FileOutputStream out = new FileOutputStream(rootDir.resolve(filename).toFile());
        while (length > 0) {
            int blockSize = length >= MainServer.BUFFER_SIZE ? MainServer.BUFFER_SIZE : (int) length;
            int bytesRead = in.read(bytes, 0, blockSize);
            out.write(bytes, 0, bytesRead);
            md.update(bytes, 0, bytesRead);
            length -= bytesRead;
        }
        return equalCheckSum(in, md.digest());
    }

    private boolean equalCheckSum(DataInputStream in, byte[] downloaded) throws IOException {
        for (int i = 0; i < GlobalSettings.CHECKSUM_LENGTH; i++) {
            if ((byte) in.read() != downloaded[i]) {
                LogService.USERS.error(login, "Checksum error");
                return false;
            }
        }
        LogService.USERS.error(login, "Checksum OK");
        return true;
    }

    private void sendLoginRegError(LoginRegError err) {
        LogService.AUTH.warn(socket.toString(), err.toString());
        sendCommand(ControlMessage.ERROR, String.valueOf(err.ordinal()));
    }

    public void sendCommand(ControlMessage m) {
        sendCommand(m, "");
    }

    public void sendCommand(ControlMessage m, String s) {
        String msg = (char) GlobalSettings.COMMAND_START_SIGNAL_BYTE + m.toString() + " " + s;
        if (!socket.isClosed()) {
            try {
                out.writeUTF(msg);
            } catch (IOException e) {
                LogService.SERVER.error("IO", e.toString());
            }
        }
    }

    public void closeIOStreams() {
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLogin() {
        return login;
    }
}
