package com.geekbrains.an.client;

import com.geekbrains.an.model.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class MainController implements Initializable {


    public ListView<String> ListView;
    public TextField TextField;
    private Path clientDir;
    public ListView<String> clientView;
    public ListView<String> serverView;
    public TextField path_Field;
    public TextField path_server_Field;

    private String BufCopy = null;
    private String BufCopy_server = null;
    private Path path_copy = null;
    private String path_copy_server = null;

    public TextField input;
    private ObjectDecoderInputStream is;
    private ObjectEncoderOutputStream os;

    public Pane regPanel;
    public Pane authPanel;
    public Pane authRegPanel;
    public Pane workPanel;
    public TextField loginFild;
    public PasswordField passwordFild;
    public TextField loginRegFild;
    public PasswordField passwordRegFild;
    public PasswordField passwordRegFild2;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {

            clientDir = Paths.get("client", "client");
            if (!Files.exists(clientDir)) {
                Files.createDirectory(clientDir);
            }

            updateClientView();
            initMouseListeners();
            Socket socket = new Socket("localhost", 8189);
            System.out.println("Network created...");
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void auth(ActionEvent actionEvent) throws IOException {
        if (loginFild != null & passwordFild != null) {
            os.writeObject(new User(loginFild.getText(), passwordFild.getText()));
        }
    }
    public void regPanelAction(ActionEvent actionEvent) {
        regPanel.setVisible(true);
        regPanel.setDisable(false);
        authPanel.setVisible(false);
        authPanel.setDisable(true);
    }

    public void regAction(ActionEvent actionEvent) throws IOException {
        if (loginRegFild.getText() != null && passwordRegFild.getText().equals(passwordRegFild2.getText())) {
            log.debug("ok", loginRegFild.getText(), passwordRegFild.getText() );
            os.writeObject(new User_reg(loginRegFild.getText(), passwordRegFild.getText()));
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "пароли не совпадают ", ButtonType.OK);
        alert.showAndWait();
        log.debug("fail " + loginRegFild.getText() + " password " + passwordRegFild.getText() + " password2 " + passwordRegFild2.getText());}
    }

    private void read() {
        try {
            while (true) {
                CloudMessage message = (CloudMessage) is.readObject();
                log.info("received: {}", message);
                switch (message.getType()) {
                    case FILE:
                        processFileMessage((FileMessage) message);
                        break;
                    case LIST:
                        processListMessage((ListMessage) message);
                        break;
                    case AUTH:
                        log.debug("auth");
                        processAuthOk();
                        break;
                    case AUTH_FAIL:
                        log.debug("auth fail");
                        processAuthFail();
                        break;
                    case REGISTER:
                        processRegOk();
                        break;
                    case REGISTER_FAIL:
                        processRegFail();
                        break;
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void processRegFail() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Неудалось зарегистрироваться, возможно такой Логин уже существует ", ButtonType.OK);
            alert.showAndWait();
        });

    }

    private void processRegOk() {
            regPanel.setDisable(true);
            regPanel.setVisible(false);
            authPanel.setDisable(false);
            authPanel.setVisible(true);
    }

    private void processAuthFail() throws IOException  {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Неправильный Логин или пароль ", ButtonType.OK);
            alert.showAndWait();
        });
    }

    private void processAuthOk() {
        authRegPanel.setVisible(false);
        authRegPanel.setDisable(true);
        authPanel.setVisible(false);
        authPanel.setDisable(true);
        workPanel.setVisible(true);
        workPanel.setDisable(false);

    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        os.writeObject(new FileMessage(clientDir.resolve(fileName)));
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        log.debug("download: {}", fileName);
        os.writeObject(new FileRequest(fileName));
    }

    private void processListMessage(ListMessage message) {
        Platform.runLater(() -> {
            serverView.getItems().clear();
            serverView.getItems().addAll(message.getFiles());
            path_server_Field.setText(message.getPathField());
        });

    }

    private void processFileMessage(FileMessage message) throws IOException {
        try {
            if (Files.exists(clientDir.resolve(message.getFileName()))) {
                if (Files.exists(clientDir.resolve("copy_" + message.getFileName()))) {
                    int i = 1;
                    while (Files.exists(clientDir.resolve("copy" + i + "_" + message.getFileName()))) {
                        i++;
                    } Files.write(clientDir.resolve("copy" + i + "_" + message.getFileName()), message.getBytes());

                } Files.write(clientDir.resolve("copy" + "_" + message.getFileName()), message.getBytes());

        } else { Files.write(clientDir.resolve(message.getFileName()), message.getBytes()); }
            Platform.runLater(this::updateClientView);

        } catch (IOException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Неудалось скачать файл ", ButtonType.OK);
                alert.showAndWait();
        }
    }

    private void updateClientView() {

            try {
                path_Field.setText(clientDir.normalize().toAbsolutePath().toString());
              clientView.getItems().clear();
              Files.list(clientDir)
                      .map(p -> p.getFileName().toString())
                      .forEach(f -> clientView.getItems().add(f));
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void initMouseListeners(){
        clientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Path current = clientDir.resolve(getItem());
                if (Files.isDirectory(current)) {
                    clientDir = current;
                    Platform.runLater(this::updateClientView);
                }
            }
        });

        serverView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String file = serverView.getSelectionModel().getSelectedItem();
                log.debug("file = ", file);
                try {
                    processInDirectori(file);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }

    private void processInDirectori(String file) throws IOException {
        os.writeObject(new Directori(file));
    }

    private String getItem() {
        return clientView.getSelectionModel().getSelectedItem();
    }

    public void btnExitAction(ActionEvent actionEvent) { Platform.exit();
    }

    public void btnDelete(ActionEvent actionEvent) {

        if (clientView.isFocused() && clientView.getSelectionModel().getSelectedItem() != null) {
            String fileName = clientView.getSelectionModel().getSelectedItem().toString();
            try {
                Files.delete(Paths.get(path_Field.getText() + "/" + fileName));
                updateClientView();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось удалить файл", ButtonType.OK);
                alert.showAndWait();
            }
        } else if (serverView.isFocused() && serverView.getSelectionModel().getSelectedItem() != null) {
            try {
                String fileName = serverView.getSelectionModel().getSelectedItem();
                os.writeObject(new DeleteFile(fileName));

            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось удалить файл", ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    public void copyFile(ActionEvent actionEvent) {
        if (!clientView.isFocused() && !serverView.isFocused()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (clientView.isFocused()) {
            if (clientView.getSelectionModel().getSelectedItem() != null) {
                BufCopy = clientView.getSelectionModel().getSelectedItem();
                path_copy = clientDir.resolve(BufCopy);
                log.debug("Copy file = {}", BufCopy);

            }
        }

        if (serverView.getSelectionModel().getSelectedItem() != null) {
            BufCopy_server = serverView.getSelectionModel().getSelectedItem();
            path_copy_server = path_server_Field.getText();
            log.debug("Copy file = {}", BufCopy_server);
            log.debug("serverView = {}", serverView.getSelectionModel().getSelectedItem());
        }
    }

    public void pasteFile(ActionEvent actionEvent) {
        if (clientView.isFocused()) {
            if (path_copy != null) {
                Path p = clientDir.resolve(BufCopy);
                Path p_copy = clientDir.resolve(BufCopy.concat("_copy"));
                try {
                    if (Files.exists(p)) {
                        Files.copy(path_copy, p_copy);
                        log.debug("Past file " + BufCopy + "copy");
                    } else {
                        Files.copy(path_copy, p);
                        log.debug("Past file " + BufCopy);
                    }
                    updateClientView();
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно скопировать ", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        }
        if (serverView.isFocused()) {
            try {
                if (path_copy_server != null) {
                    os.writeObject(new PasteFile(BufCopy_server, path_copy_server));
                }
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно скопировать ", ButtonType.OK);
                alert.showAndWait();
                e.printStackTrace();
            }
        }
    }

    public void rename_file(ActionEvent actionEvent) {
        if (clientView.isFocused()) {
            String s = clientDir.resolve(clientView.getSelectionModel().getSelectedItem()).toString();
            File oldName = new File(s);
            log.debug("oldname " + s);
            TextInputDialog dialog = new TextInputDialog("Имя");
            dialog.setTitle("Новое имя");
            dialog.setHeaderText("Введите имя : ");
            dialog.setContentText("Пожалуйста, введите новое имя: ");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newName -> {
                log.debug("newName " + newName);
                if (oldName.renameTo(new File(clientDir.resolve(newName).toString()))) {
                    log.debug("Файл переименован успешно");
                } else {
                    log.debug("Файл не был переименован");
                }
                updateClientView();
            });
        } else if (serverView.isFocused()) {
            String s = serverView.getSelectionModel().getSelectedItem();
            log.debug("oldname " + s);
            TextInputDialog dialog = new TextInputDialog("Имя");
            dialog.setTitle("Новое имя");
            dialog.setHeaderText("Введите имя : ");
            dialog.setContentText("Пожалуйста, введите новое имя: ");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newName -> {
                log.debug("newName " + newName);
                try {
                    os.writeObject(new RenameFile(s, newName));
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно переименовать файл ", ButtonType.OK);
                alert.showAndWait();
                    e.printStackTrace();
                }
            });
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не один файл не выбран ", ButtonType.OK);
                alert.showAndWait();
        }
    }

    public void create_folder(ActionEvent actionEvent) {
        if (clientView.isFocused()) {
            TextInputDialog dialog = new TextInputDialog("Новая папка");
            dialog.setTitle("Имя папки");
            dialog.setHeaderText("Введите имя новой папки: ");
            dialog.setContentText("Пожалуйста, введите имя новой папки: ");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                try {
                    if (Files.exists(clientDir.resolve(name))) {
                        int i = 1;
                        while (Files.exists(clientDir.resolve(name + i))) {
                            i++;
                        }
                        Files.createDirectory(clientDir.resolve(name + i));
                        log.debug("Create directory  " + name + i);
                    } else {Files.createDirectory(clientDir.resolve(name));}
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Неудалось создать папку ", ButtonType.OK);
                    alert.showAndWait();
                    e.printStackTrace();
                }
            });

            Platform.runLater(this::updateClientView);
        } else if (serverView.isFocused()) {
            TextInputDialog dialog = new TextInputDialog("Новая папка");
            dialog.setTitle("Имя папки");
            dialog.setHeaderText("Введите имя новой папки: ");
            dialog.setContentText("Пожалуйста, введите имя новой папки: ");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                try {
                    os.writeObject(new CreateFolder(name));
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Неудалось создать папку ", ButtonType.OK);
                    alert.showAndWait();
                    e.printStackTrace();
                }
            });
        }
    }

    public void sendMessage(ActionEvent actionEvent) {

    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = clientDir.getParent();
        if (upperPath != null) {
            clientDir = upperPath;
            Platform.runLater(this::updateClientView);
        }

    }

    public void btnPathUpAction_server(ActionEvent actionEvent) throws IOException {
        log.debug("btn_up: {} ", path_server_Field.getText());
        os.writeObject(new UpDirectori());

    }

    public void ClientHomeClik(ActionEvent actionEvent) {
        clientDir = Paths.get("client", "client");
        Platform.runLater(this::updateClientView);
    }

    public void ServerHomeClik(ActionEvent actionEvent) throws IOException {
        os.writeObject(new ServerHome());
    }


    public void backToLoginAuth(ActionEvent actionEvent) {
        regPanel.setDisable(true);
        regPanel.setVisible(false);
        authPanel.setDisable(false);
        authPanel.setVisible(true);
    }
}
