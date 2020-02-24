package ru.kornev.cloudserver.services.console;

import ru.kornev.cloudserver.app.MainServer;
import ru.kornev.cloudserver.app.handlers.ClientDataHandler;
import ru.kornev.cloudserver.services.LogService;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class ConsoleHandler {
    private MainServer server;
    private TrayIcon trayIcon;

    public ConsoleHandler(MainServer server) {
        this.server = server;
        runConsoleHandler();
        setTrayIcon();
    }

    private void runConsoleHandler() {
        Thread consoleThread = new Thread(() -> {
            BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));
            String consoleString;
            try {
                while (true) {
                    consoleString = consoleIn.readLine();
                    if (consoleString.trim().isEmpty() || !CommandMessage.isControlMessage(consoleString)) continue;
                    consoleString = CommandMessage.getCommand(consoleString);
                    if (CommandMessage.CLOSE_SERVER.check(consoleString)) break;
                    else if (CommandMessage.USER_LIST.check(consoleString)) printUsersList();
                }
            } catch (IOException e) {
                LogService.SERVER.error(e.toString());
            } finally {
                closeServer();
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    private void setTrayIcon() {
        if (!SystemTray.isSupported()) return;
        PopupMenu menu = new PopupMenu();
        trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/img/icon32x32.png")));
        trayIcon.setImageAutoSize(true);
        MenuItem menuClose = new MenuItem("Close");
        menuClose.addActionListener(e -> closeServer());
        menu.add(menuClose);
        trayIcon.setPopupMenu(menu);
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            LogService.SERVER.error("Can't add tray icon");
        }
    }

    private void closeServer() {
        SystemTray.getSystemTray().remove(trayIcon);
        server.closeChannel();
    }

    private void printUsersList() {
        List<ClientDataHandler> list = server.getClients().stream().filter(clientHandler -> clientHandler.getLogin() != null).collect(Collectors.toList());
        if (list.size() == 0) {
            System.out.println("No users online");
            return;
        }
        System.out.println("Users online:");
        list.forEach(clientHandler -> System.out.println(clientHandler.getLogin()));
    }
}
