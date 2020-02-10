package services;

import app.ClientHandler;
import app.MainServer;
import resources.CommandMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class ConsoleHandler {
    private MainServer server;

    public ConsoleHandler(MainServer server) {
        this.server = server;
    }

    private void runConsoleHandler() {
        Thread consoleThread = new Thread(() -> {
            BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));
            String consoleString;
            try {
                while (true) {
                    consoleString = consoleIn.readLine();
                    if (consoleString.trim().isEmpty() || !consoleString.startsWith("/")) continue;
                    consoleString = getConsoleCommand(consoleString);
                    if (CommandMessage.CLOSE_SERVER.check(consoleString)) break;
                    else if (CommandMessage.USER_LIST.check(consoleString)) printUsersList();
                }
            } catch (IOException e) {
                LogService.SERVER.error(e.toString());
            } finally {
                server.serverShutDown();
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    private String getConsoleCommand(String string) {
        return string.replaceFirst("/", "");
    }

    private void printUsersList() {
        List<ClientHandler> list = server.getClients().stream().filter(clientHandler -> clientHandler.getLogin() != null).collect(Collectors.toList());
        if (list.size() == 0) {
            System.out.println("No users online");
            return;
        }
        System.out.println("Users online:");
        list.forEach(clientHandler -> System.out.println(clientHandler.getLogin()));
    }
}
