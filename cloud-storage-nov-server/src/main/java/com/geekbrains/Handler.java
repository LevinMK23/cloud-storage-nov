package com.geekbrains;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler implements Runnable {

    private static int BUFFER_SIZE = 1024;
    private byte[] buf;
    private static int counter = 0;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataInputStream is;
    private final DataOutputStream os;
    private final String name;
    private boolean isRunning;
    private Path serverDir;

    public Handler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        counter++;
        name = "User#" + counter;
        log.debug("Set nick: {} for new client", name);
        isRunning = true;
        buf = new byte[BUFFER_SIZE];
        serverDir = Paths.get("cloud-storage-nov-server", "server");
        sendListFiles();
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
                    processFileMessage();
                }
                if (command.equals("fileRequest")) {
                    processFileRequest();
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void processFileRequest() throws IOException {
        String fileName = is.readUTF();
        os.writeUTF("file");
        os.writeUTF(fileName);
        long size = Files.size(serverDir.resolve(fileName));
        os.writeLong(size);
        Path file = serverDir.resolve(fileName);
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            while (fis.available() > 0) {
                int read = fis.read(buf);
                os.write(buf, 0, read);
            }
        }
        os.flush();
    }

    private void processFileMessage() throws IOException {
        String fileName = is.readUTF();
        log.debug("download file: {}", fileName);
        long size = is.readLong();
        log.debug("Read {} bytes", size);
        long butchCount = (size + BUFFER_SIZE - 1) / BUFFER_SIZE;
        Path file = serverDir.resolve(fileName);
        try (OutputStream fos = new FileOutputStream(file.toFile())) {
            for (int i = 0; i < butchCount; i++) {
                int read = is.read(buf);
                fos.write(buf, 0, read);
            }
        }
        sendListFiles();
    }

    private void sendListFiles() throws IOException {
        List<String> files = Files.list(serverDir).map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        os.writeUTF("list");
        os.writeInt(files.size());
        for (String file : files) {
            os.writeUTF(file);
        }
        os.flush();
    }
}
