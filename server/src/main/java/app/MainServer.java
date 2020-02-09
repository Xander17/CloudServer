package app;

import resources.ControlMessage;
import services.DatabaseSQL;
import services.LogService;
import settings.GlobalSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class MainServer {

    public static final int BUFFER_SIZE = 8192;
    public static final Path REPOSITORY_ROOT = Paths.get("server-repo");
    private DatabaseSQL db;
    private ServerSocket serverSocket;
    private Vector<ClientHandler> clients = new Vector<>();

    public MainServer() {
        runServer();
        runConsoleHandler();
    }

    private void runServer() {
        db = DatabaseSQL.getInstance();
        db.connect();
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(GlobalSettings.CONNECTION_PORT);
                if (Files.notExists(REPOSITORY_ROOT)) Files.createDirectory(REPOSITORY_ROOT);
                LogService.SERVER.info("Server started.");
                while (true) {
                    Socket socket = serverSocket.accept();
                    addClient(socket);
                    LogService.SERVER.info("New client connected. " + socket + ". " + getConnectionsCountInfo());
                }
            } catch (IOException e) {
                LogService.SERVER.error(e.toString());
            } finally {
                serverShutDown();
            }
        }).start();
    }

    private void addClient(Socket socket) {
        clients.add(new ClientHandler(this, socket));
    }

    public void deleteClient(ClientHandler client) {
        LogService.SERVER.info("Client disconnected.", "Login", client.getLogin(), getConnectionsCountInfo());
        clients.remove(client);
    }

    public boolean isUserOnline(String login) {
        for (ClientHandler client : clients) {
            if (client.getLogin().equals(login)) return true;
        }
        return false;
    }

    private String getConnectionsCountInfo() {
        return "Total connected clients: " + clients.size();
    }

    private void runConsoleHandler() {
        Thread consoleThread = new Thread(() -> {
            BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));
            String consoleString;
            try {
                while (true) {
                    consoleString = consoleIn.readLine();
                    if (consoleString.trim().isEmpty()) continue;
                    if (consoleString.equalsIgnoreCase(ControlMessage.CLOSE_SERVER.toString())) break;
                }
            } catch (IOException e) {
                LogService.SERVER.error(e.toString());
            } finally {
                serverShutDown();
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    private void serverShutDown() {
// TODO: 09.02.2020 закрытие соединений
        db.shutdown();
        LogService.SERVER.info("Server shutdown.");

//        try {
//            clients.forEach(app.ClientHandler::closeIOStreams);
//            DatabaseSQL.shutdown();
//            if (!server.isClosed()) {
//                server.close();
//                LogService.SERVER.info("Server stopped.");
//            }
//        } catch (IOException e) {
//            LogService.SERVER.error(e.getMessage());
//        }
    }
}
