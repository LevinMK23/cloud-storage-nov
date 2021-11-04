package com.geekbrains.chat;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.SneakyThrows;

public class ChatController implements Initializable {

    public TextField input;
    public ListView<String> list;
    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Socket socket = new Socket("localhost", 8189);
        os = new ObjectEncoderOutputStream(socket.getOutputStream());
        is = new ObjectDecoderInputStream(socket.getInputStream());
        Thread thread = new Thread(this::read);
        thread.setDaemon(true);
        thread.start();

    }

    @SneakyThrows
    private void read() {
        while (true) {
            String msg = (String) is.readObject();
            Platform.runLater(() -> list.getItems().add(msg));
        }
    }


    public void send(ActionEvent actionEvent) throws IOException {
        os.writeObject(input.getText());
        os.flush();
    }
}
