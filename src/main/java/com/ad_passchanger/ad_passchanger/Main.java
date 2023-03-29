package com.ad_passchanger.ad_passchanger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/Main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("AD PAssChanger");
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/login.png")));
        stage.getIcons().add(icon);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}