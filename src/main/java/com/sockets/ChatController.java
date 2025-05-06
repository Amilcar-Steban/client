package com.sockets;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

public class ChatController {

    @FXML private TextField usernameField;
    @FXML private TextField serverAddressField;
    @FXML private Button connectButton;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private VBox messageContainer;
    @FXML private Circle connectionStatus;
    @FXML private Label statusLabel;
    @FXML private ScrollPane messageScrollPane;
    @FXML private Button attachButton;
    @FXML private HBox fileOptionsPanel;
    @FXML private Button sendImageButton;
    @FXML private Button sendFileButton;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private volatile boolean connected = false;

    @FXML
    public void initialize() {
        serverAddressField.setText("localhost:8081");
        connectButton.setOnAction(e -> handleConnect());
        sendButton.setDisable(true);
        connectionStatus.setFill(Color.RED);
        statusLabel.setText("Desconectado");

        // Configurar el botón de adjuntar
        attachButton.setOnAction(e -> {
            toggleFileOptions();
        });
        
        // Configurar botones de opciones de archivo
        sendImageButton.setOnAction(e -> handleSendImage());
        sendFileButton.setOnAction(e -> handleSendFile());
        
        // Ocultar panel inicialmente
        fileOptionsPanel.setVisible(false);
    }

    private void toggleFileOptions() {
        fileOptionsPanel.setVisible(!fileOptionsPanel.isVisible());
        
        // Asegurarse de que el panel esté en primer plano
        if (fileOptionsPanel.isVisible()) {
            fileOptionsPanel.toFront();
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    private void handleConnect() {
        
        if (connected) {
            disconnect();
        } else {
            String username = usernameField.getText().trim();
            String address = serverAddressField.getText().trim();

            if (username.isEmpty() || address.isEmpty()) {
                showAlert("Error", "Debes ingresar tu nombre y dirección del servidor.");
                return;
            }

            try {
                String[] parts = address.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                socket = new Socket(host, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Enviar nombre apenas conecta
                out.println(username);

                connected = true;
                updateUIOnConnection(true);

                // Hilo para escuchar mensajes
                listenerThread = new Thread(this::listenForMessages);
                listenerThread.start();

            } catch (Exception ex) {
                showAlert("Error de conexión", ex.getMessage());
                cleanup();
            }
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                final String incomingMessage = message;
                Platform.runLater(() -> addMessage(incomingMessage));
            }
        } catch (IOException e) {
            System.err.println("Error recibiendo mensajes: " + e.getMessage());
        } finally {
            Platform.runLater(() -> {
                disconnect();
            });
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleSendMessage() {
        if (!connected) return;

        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // Mostrar el mensaje localmente primero
            String formattedMessage = usernameField.getText() + ": " + message;
            addMessage(formattedMessage);
            
            // Enviar el mensaje al servidor
            out.println(message);
            messageField.clear();
        }
    }

    private void addMessage(String rawMessage) {
        // Determinar si el mensaje es propio (contiene el nombre de usuario)
        boolean isMyMessage = rawMessage.startsWith(usernameField.getText() + ":");
        
        Label label = new Label(rawMessage);
        label.setWrapText(true);
        
        // Aplicar estilos según el tipo de mensaje
        if (isMyMessage) {
            label.getStyleClass().addAll("message-bubble", "user-message");
        } else {
            label.getStyleClass().addAll("message-bubble", "other-message");
        }
        
        HBox wrapper = new HBox(label);
        wrapper.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setMaxWidth(Double.MAX_VALUE); // Para que ocupe todo el ancho disponible
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        
        messageContainer.getChildren().add(wrapper);
        messageScrollPane.setVvalue(1.0); // Auto-scroll al final
    }

    private void updateUIOnConnection(boolean connectedNow) {
        connectionStatus.setFill(connectedNow ? Color.GREEN : Color.RED);
        sendButton.setDisable(!connectedNow);
        connectButton.setText(connectedNow ? "Desconectar" : "Conectar");
        usernameField.setDisable(connectedNow);
        serverAddressField.setDisable(connectedNow);
        statusLabel.setText(connectedNow ? "Conectado" : "Desconectado");
    }

    private void disconnect() {
        connected = false;
        if (out != null) out.println("/exit");

        cleanup();
        updateUIOnConnection(false);
    }

    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando recursos: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void handleSendFile() {
        toggleFileOptions();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        File file = fileChooser.showOpenDialog(messageField.getScene().getWindow());
        
        if (file != null) {
            sendFile(file);
        }
    }

    private void handleSendImage() {
        toggleFileOptions();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar imagen");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        File file = fileChooser.showOpenDialog(messageField.getScene().getWindow());
        
        if (file != null) {
            sendFile(file);
        }
    }

    private void sendFile(File file) {
        if (!connected) return;
        
        try {
            // Aquí implementarías la lógica para enviar el archivo a través del socket
            // Esto es solo un ejemplo básico
            
            // Primero notificar al servidor que se enviará un archivo
            out.println("/file " + file.getName() + " " + file.length());
            
            // Luego enviar el archivo (necesitarías implementar esto adecuadamente)
            // Por ejemplo, podrías usar un FileInputStream y enviar los bytes
            
            // Mostrar notificación local
            String formattedMessage = usernameField.getText() + ": " + file.getName();
            Platform.runLater(() -> {
                addMessage(formattedMessage);
            });
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                showAlert("Error", "No se pudo enviar el archivo: " + e.getMessage());
            });
        }
    }

}
