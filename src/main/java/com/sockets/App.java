package com.sockets;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sockets/primary.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/sockets/styles.css").toExternalForm());

            primaryStage.setResizable(false);
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                primaryStage.initStyle(StageStyle.UNDECORATED);
                primaryStage.initStyle(StageStyle.DECORATED);
            }
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMaxWidth(800);
            primaryStage.setMinHeight(850);
            primaryStage.setMaxHeight(850);

            primaryStage.setOnCloseRequest(event -> {
                System.out.println("Cerrando aplicación...");
                System.exit(0);
            });

            primaryStage.show();
        } catch (IOException e) {
            throw e;
        }
    }

    public static void main(String[] args) {
        System.setProperty("javafx.debug", "true");
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Error no capturado en hilo " + thread.getName());
        });
        
        launch(args);
    }
}