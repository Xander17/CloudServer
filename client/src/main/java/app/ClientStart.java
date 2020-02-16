package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ClientStart extends Application {

    public static void main(String[] args) {
        //new app.Client();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        String DEFAULT_STYLE = "css/client.css";

        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/client.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("GB Cloud");
        primaryStage.setScene(new Scene(root, 800, 700));
        primaryStage.setMinWidth(300);
        primaryStage.setMinHeight(300);
        primaryStage.getScene().getStylesheets().add("css/base_style.css");
        primaryStage.getScene().getStylesheets().add("css/gradient_style.css");
        primaryStage.getScene().getStylesheets().add(DEFAULT_STYLE);
        primaryStage.getIcons().add(new Image("img/icon.png"));
        primaryStage.setOnCloseRequest(e -> controller.exitApp());
//        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.show();
    }
}
