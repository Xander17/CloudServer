package ru.kornev.cloudclient.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ru.kornev.cloudclient.resources.ClientSettings;
import ru.kornev.cloudcommon.services.settings.Settings;

public class ClientStart extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Settings.load("client.cfg", ClientSettings.getSettings());
        String stylePath = Settings.get(ClientSettings.STYLE);

        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/client.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        primaryStage.setTitle("GB Cloud");
        primaryStage.setScene(new Scene(root, 900, 700));
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(500);
        primaryStage.getScene().getStylesheets().add("css/base_style.css");
        primaryStage.getScene().getStylesheets().add("css/gradient_style.css");
        primaryStage.getScene().getStylesheets().add(stylePath);
        primaryStage.getIcons().add(new Image("img/icon.png"));
        primaryStage.setOnCloseRequest(e -> controller.exitApp());
        primaryStage.show();
    }
}
