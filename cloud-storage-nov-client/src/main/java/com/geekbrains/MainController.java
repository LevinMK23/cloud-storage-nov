package com.geekbrains;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainController implements Initializable {

    private static final int BUFFER_SEZE = 1024;
    private byte [] buf;
    private Path clientDir;
    public ListView<String> clientView;
    public ListView<String> serverView;
    public TextField input;
    private DataInputStream is;
    private DataOutputStream os;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            buf = new byte[BUFFER_SEZE];
            clientDir = Paths.get("cloud-storage-nov-client", "client");
            if (!Files.exists(clientDir)) {
                Files.createDirectory(clientDir);
            }

            clientView.getItems().clear();
            clientView.getItems().addAll(getFiles(clientDir));
            clientView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    String item = clientView.getSelectionModel().getSelectedItem();
                    input.setText(item);
                }
            });
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> getFiles(Path path) throws IOException {
        // web 4 Stream Api
        return Files.list(path)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }

    private void read() {
        try {
            while (true) {
                String command = is.readUTF();
                log.debug("Received: {}", command);
                if (command.equals("list")) {
                    int count = is.readInt();
                    log.debug("Procces list files, count = {}", count);
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        String fileName = is.readUTF();
                        list.add(fileName);
                    }
                    Platform.runLater(() -> {
                        serverView.getItems().clear();
                        serverView.getItems().addAll(list);
                    });
                    log.debug("files on server: {}", list);
                }
                if (command.equals("file")) {
                    String fileName = is.readUTF();
                    log.debug("download file: {}", fileName);
                    long size = is.readLong();
                    long butchCount = (size + BUFFER_SEZE - 1)/BUFFER_SEZE;
                    Path file = clientDir.resolve(fileName);
                    try (OutputStream fos = new FileOutputStream(file.toFile())) {
                        for (int i = 0; i < size; i++) {
                            int read = is.read(buf);
                            fos.write(buf, 0, read);

                        }
                    }
                    List<String> files = getFiles(clientDir);
                    Platform.runLater(()-> {
                        clientView.getItems().clear();
                        clientView.getItems().addAll(files);
                    });

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(ActionEvent actionEvent) throws IOException {
        String text = input.getText();
        os.writeUTF(text);
        os.flush();
        input.clear();
        // TODO: 28.10.2021 Передать файл на сервер
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        os.writeUTF("file");
        os.writeUTF(fileName);
        long size = Files.size(clientDir.resolve(fileName));
        os.writeLong(size);
        Path file = clientDir.resolve(fileName);
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            while (fis.available() > 0) {
                int read = fis.read(buf);
                os.write(buf, 0 , read);
            }
        }
        os.flush();
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        os.writeUTF("fileRequest");
        os.writeUTF(fileName);
        os.flush();

    }
}
