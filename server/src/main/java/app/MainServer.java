package app;

import services.ConsoleHandler;
import services.DatabaseSQL;
import services.LogService;
import settings.GlobalSettings;

import java.io.IOException;
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
        new ConsoleHandler(this);
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
        clients.remove(client);
        LogService.SERVER.info("Client disconnected.", "Login", client.getLogin(), getConnectionsCountInfo());
    }

    public boolean isUserOnline(String login) {
        for (ClientHandler client : clients) {
            if (client.getLogin() == null) continue;
            if (client.getLogin().equals(login)) return true;
        }
        return false;
    }

    private String getConnectionsCountInfo() {
        return "Total connected clients: " + clients.size();
    }

    public void serverShutDown() {
        try {
            clients.forEach(ClientHandler::closeIOStreams);
            serverSocket.close();
            db.shutdown();
        } catch (IOException e) {
            LogService.SERVER.error("Shutdown error", e.toString());
        } finally {
            LogService.SERVER.info("Server shutdown.");
        }
    }

    public Vector<ClientHandler> getClients() {
        return clients;
    }
}
