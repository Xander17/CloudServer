package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        String DEFAULT_STYLE = "css/client.css";

        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("fxml/client.fxml"));
        primaryStage.setTitle("GB Cloud");
        primaryStage.setScene(new Scene(root, 800, 700));
        primaryStage.setMinWidth(300);
        primaryStage.setMinHeight(300);
        primaryStage.getScene().getStylesheets().add(DEFAULT_STYLE);
        primaryStage.getIcons().add(new Image("img/icon.png"));
//        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.show();
    }


    public static void main(String[] args) {
        //new app.Client();
        launch(args);
    }
}
