package com.geekbrains;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class MainController implements Initializable {


    private static final int BUFFER_SEZE = 1024;
    private byte [] buf;
    private String BufCopy = null;
     private String BufCopy_server = null;
    private Path path_copy = null;
    private String path_copy_server = null;
    private Path clientDir;
    public TableView<File_Info> clientView2;
    public ListView<String> clientView;
    public TableView<File_Info> serverView2;
    public ListView<String> serverView;
    public TextField path_Field;
    public TextField path_server_Field;


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
        TableColumn<File_Info, String> fileTypeColumn = new TableColumn<>("Тип");
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(30);

        TableColumn<File_Info, String> fileNameColumn = new TableColumn<>("Имя");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        fileNameColumn.setPrefWidth(150);

        TableColumn<File_Info, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));

        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<File_Info, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(110);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<File_Info, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(150);

        clientView2.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn);
        clientView2.getSortOrder().add(fileTypeColumn);

            updateList(clientDir);
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
            serverView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    String file = path_server_Field.getText().concat("/" + serverView.getSelectionModel().getSelectedItem());
                    log.debug("Dable clicket to = {}", file);
                    try {
                        inDirectory(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
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

                if (command.equals("path_list")) {
                    String path_list = is.readUTF();
                    Platform.runLater(() -> {
                        path_server_Field.setText(path_list);
                    });
                }


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
                    log.debug("Read {} bytes ", size);
                    long butchCount = (size + BUFFER_SEZE - 1)/BUFFER_SEZE;
                    Path file = Paths.get(path_Field.getText()).resolve(fileName);
                    try (OutputStream fos = new FileOutputStream(file.toFile())) {
                        for (int i = 0; i < butchCount; i++) {
                            int read = is.read(buf);
                            fos.write(buf, 0, read);

                        }
                    }
                    List<String> files = getFiles( Paths.get(path_Field.getText()));
                    updateList( Paths.get(path_Field.getText()));

                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void inDirectory(String file) throws IOException {
        os.writeUTF("in_directory");
        os.writeUTF("/" + file);
        os.flush();
    }

    public void sendMessage(ActionEvent actionEvent) throws IOException {
        String text = input.getText();
        os.writeUTF(text);
        os.flush();
        input.clear();
        // TODO: 28.10.2021 Передать файл на сервер
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String fileName = clientView2.getSelectionModel().getSelectedItem().getFilename();
        os.writeUTF("file");
        os.writeUTF(path_server_Field.getText());
        os.writeUTF(fileName);
         long size = Files.size(Paths.get(path_Field.getText()).resolve(fileName));

//        long size = Files.size(clientDir.resolve(fileName));
        os.writeLong(size);
        Path file = Paths.get(path_Field.getText()).resolve(fileName);
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
        os.writeUTF(path_server_Field.getText());
        os.flush();

    }
public void updateList(Path path) {

        try {
            path_Field.setText(path.normalize().toAbsolutePath().toString());
            clientView2.getItems().clear();
            clientView2.getItems().addAll(Files.list(path).map(File_Info::new).collect(Collectors.toList()));
            clientView2.sort();
            clientView2.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Path p = Paths.get(path_Field.getText()).resolve(clientView2.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(p)) {
                        updateList(p);
                    }
                }
            });
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }
//    public void updateList(Path path) {
//
//        try {
//            path_Field.setText(path.normalize().toAbsolutePath().toString());
//            clientView.getItems().clear();
//            clientView.getItems().addAll(getFiles(path));
////            clientView.setOnMouseClicked(event -> {
////                if (event.getClickCount() == 2) {
////                    String item = clientView.getSelectionModel().getSelectedItem();
////                    input.setText(item);
////                }
////            });
//            clientView.setOnMouseClicked(event -> {
//                if (event.getClickCount() == 2) {
//                    Path p = Paths.get(path_Field.getText()).resolve(clientView.getSelectionModel().getSelectedItem());
//                    if (Files.isDirectory(p)) {
//                        updateList(p);
//                    }
//                }
//            });
//        } catch (IOException e) {
//            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
//            alert.showAndWait();
//        }
//    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(path_Field.getText()).getParent();
        if (upperPath != null) {
            updateList(upperPath);
        }
    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void btnDelete(ActionEvent actionEvent) {

        if (clientView2.isFocused() && clientView2.getSelectionModel().getSelectedItem().getFilename() != null) {
            String fileName = clientView2.getSelectionModel().getSelectedItem().getFilename();
            try {
                Files.delete(Paths.get(path_Field.getText() + "/" + fileName));
                updateList(Paths.get(path_Field.getText()));
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось удалить файл", ButtonType.OK);
                alert.showAndWait();
            }
        } else if (serverView.isFocused() && serverView.getSelectionModel().getSelectedItem() != null) {
            try {
                String fileName = serverView.getSelectionModel().getSelectedItem();
                os.writeUTF("delete_file");
                os.writeUTF(path_server_Field.getText() + "/" + fileName);
                os.flush();

            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось удалить файл", ButtonType.OK);
                alert.showAndWait();
            }
        }
        }

    public void btnPathUpAction_server(ActionEvent actionEvent) throws IOException {
        String path = path_server_Field.getText();
        log.debug("UP path = {}", path);
        os.writeUTF("btn_up");
        os.writeUTF(path);
        os.flush();
    }

    public void rename_file(ActionEvent actionEvent) {


    }

    public void copyFile(ActionEvent actionEvent) {
        if (!clientView2.isFocused() && !serverView.isFocused()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (clientView2.isFocused()) {
            if (clientView2.getSelectionModel().getSelectedItem().getFilename() != null) {
                BufCopy = clientView2.getSelectionModel().getSelectedItem().getFilename();
                path_copy = Paths.get(path_Field.getText()).resolve(BufCopy);
                log.debug("Copy file = {}", BufCopy);

            }
        }

        if (serverView.getSelectionModel().getSelectedItem() != null) {
            BufCopy_server = serverView.getSelectionModel().getSelectedItem();
            path_copy_server = path_server_Field.getText();
            log.debug("Copy file = {}", BufCopy_server);
            log.debug("clientView2 = {}", clientView2.isFocused());
            log.debug("serverView = {}", serverView.getSelectionModel().getSelectedItem());
        }
    }

    public void pasteFile(ActionEvent actionEvent) throws IOException {
        if (clientView2.isFocused()) {
            if (path_copy != null) {
                Path p = Paths.get(path_Field.getText()).resolve(BufCopy);
                Path p_copy = Paths.get(path_Field.getText()).resolve(BufCopy.concat("_copy"));
                try {
                    if (Files.exists(p)) {
                        Files.copy(path_copy, p_copy);
                        log.debug("Past file " + BufCopy + "copy");
                    } else {
                        Files.copy(path_copy, p);
                        log.debug("Past file " + BufCopy);
                    }
                    updateList(Paths.get(path_Field.getText()));
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно скопировать ", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        }
        if (serverView.isFocused()) {
             String path_to_copy = path_server_Field.getText();
            if (path_copy_server != null) {
                os.writeUTF("copy_file");

                os.writeUTF(path_copy_server);
                os.writeUTF(BufCopy_server);
                os.writeUTF(path_to_copy);
                os.flush();
            }
        }

    }

    public void create_folder(ActionEvent actionEvent) {
        try {
            if (clientView2.isFocused()) {
            if (Files.exists(Paths.get(path_Field.getText() + "/" + "Новая папка"))) {
                int i = 1;
                while (Files.exists(Paths.get(path_Field.getText() + "/" + "Новая папка" + i))) {
                    i++;
                }
                Files.createDirectory(Paths.get(path_Field.getText() + "/" + "Новая папка" + i));
                log.debug("Create directory  " + "Новая папка" + i);
            } else {
                Files.createDirectory(Paths.get(path_Field.getText() + "/" + "Новая папка"));
            }

                updateList(Paths.get(path_Field.getText()));
            } else if (serverView.isFocused()) {
                String path = path_server_Field.getText();
                os.writeUTF("crt_folder");
                log.debug("create folder in path = {}", path);
                os.writeUTF(path);
                os.flush();
            }

    } catch (IOException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Неудалось создать папку ", ButtonType.OK);
                alert.showAndWait();
}
    }
}
