package com.geekbrains;

import javafx.event.ActionEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Handler implements Runnable {

    private static final int BUFFER_SEZE = 1024;
    private static int counter = 0;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataInputStream is;
    private final DataOutputStream os;
    private final String name;
    private boolean isRunning;
    private byte[] buf;
    private Path serverDir;

    public Handler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        counter++;
        name = "User#" + counter;
        log.debug("Set nick: {} for new client", name);
        isRunning = true;
        buf = new byte[BUFFER_SEZE];
        serverDir = Paths.get("cloud-storage-nov-server", "server").toAbsolutePath();
        sendListFiles(serverDir);
    }
    private String getDate() {
        return formatter.format(LocalDateTime.now());
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                String command = is.readUTF(); // wait data
                log.debug("received: {}", command);
                if (command.equals("file")) {
                    Path path = Paths.get(is.readUTF()).normalize();
                    processFileMessage(path);
                }
                if (command.equals("fileRequest")) {
                    processFileRequest();
                }
                if (command.equals("btn_up")) {
                    Path path = Paths.get(is.readUTF()).normalize();
                    log.debug("Up path: {}", path);
                    btnPathUpAction_server(path);
                }
                if (command.equals("in_directory")) {
                    Path path = Paths.get(is.readUTF()).normalize();
                    log.debug("Dable clicket to = {}", path);
                    if (Files.isDirectory(path)) {
                        sendListFiles(path);
                    }
                }
                if (command.equals("crt_folder")) {
                    Path path = Paths.get(is.readUTF()).normalize();
                    create_folder_server(path);
                }
                if (command.equals("delete_file")) {
                    String fileName = is.readUTF();
                    try {
                Files.delete(Paths.get(fileName).normalize());
                sendListFiles(Paths.get(fileName).getParent());
            } catch (IOException e) {
                log.debug("Не удалось удалить файл = {}", fileName);
                    }
                }
                if (command.equals("copy_file")) {
                    Path path = Paths.get(is.readUTF()).normalize();
                    copyPasteFile(path);
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void copyPasteFile(Path path) throws IOException {
        String filename_copy = is.readUTF();
        String path_to_copy = is.readUTF();
        Path Path_to_copy = Paths.get(path_to_copy).normalize().resolve(filename_copy);
        Path p_copy = Paths.get(path_to_copy).normalize().resolve(filename_copy + "_copy");
        try {
                    if (Files.exists(Path_to_copy)) {
                        Files.copy(path, p_copy);
                        log.debug("Past file = {}", p_copy);
                    } else {
                        Files.copy(path, Path_to_copy);
                        log.debug("Past file = {}", path_to_copy);
                    }
                    sendListFiles(Paths.get(path_to_copy));
                } catch (IOException e) {
            log.debug("Не удалось скопировать файл = {}", filename_copy );
        }



    }

    private void create_folder_server(Path path) {
        try {
        if (Files.exists(Paths.get(path + "/" + "Новая папка"))) {
                int i = 1;
                while (Files.exists(Paths.get(path + "/" + "Новая папка" + i))) {
                    i++;
                }
                Files.createDirectory(Paths.get(path + "/" + "Новая папка" + i));
                log.debug("Create directory  " + "Новая папка" + i);
            } else {
                Files.createDirectory(Paths.get(path + "/" + "Новая папка"));
                log.debug("Create directory  " + "Новая папка");
            }
            sendListFiles(path);
            } catch (IOException e) {
            log.debug("Не удалось создать папку" );
        }
    }

    private void processFileRequest() throws IOException {
        String fileName = is.readUTF();
        Path path_fild = Paths.get(is.readUTF()).normalize();
        os.writeUTF("file");
        os.writeUTF(fileName);
        long size = Files.size(path_fild.resolve(fileName));
        os.writeLong(size);
        Path file = path_fild.resolve(fileName);
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            while (fis.available() > 0) {
                int read = fis.read(buf);
                os.write(buf, 0 , read);
            }
        }
        os.flush();
    }

    private void processFileMessage(Path path) throws IOException {
        String fileName = is.readUTF();
        log.debug("download file: {}", fileName);
        long size = is.readLong();
        log.debug("Read {} bytes", size);
        long butchCount = (size + BUFFER_SEZE - 1)/BUFFER_SEZE;
        Path file = path.resolve(fileName);
        try (OutputStream fos = new FileOutputStream(file.toFile())) {
            for (int i = 0; i < butchCount; i++) {
                int read = is.read(buf);
                fos.write(buf, 0, read);

            }
        }
        sendListFiles(path);
    }
    private void sendListFiles(Path path) throws IOException {

        List<String> files = Files.list(path).map(p -> p.getFileName().toString()).collect(Collectors.toList());

        String path_list = path.normalize().toAbsolutePath().toString();
        os.writeUTF("path_list");
        os.writeUTF(path_list);
        os.flush();

        os.writeUTF("list");
        os.writeInt(files.size());
        for (String file : files) {
            os.writeUTF(file);
        }
        os.flush();
    }

    private void btnPathUpAction_server(Path path) throws IOException {

        Path upperPath = path.getParent();
        log.debug("upperPath = {}", upperPath);
        if (upperPath != null) {
            if (path.equals(serverDir)) {
                sendListFiles(serverDir);
            } else {
                sendListFiles(upperPath);
            }
        }
    }

}
