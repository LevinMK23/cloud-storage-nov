package com.geekbrains;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler implements Runnable {

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
        serverDir = Paths.get("cloud-storage-nov-server", "client_" + name);
        if (!Files.exists(serverDir)) {
            Files.createDirectory(serverDir);
        }

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
                String msg = is.readUTF(); // wait data
                log.debug("received: {}", msg);
                if (msg.startsWith("FILE")){
                    getFileFromClient();
                    continue;
                }
                String response = String.format("%s %s: %s", getDate(), name, msg);
                log.debug("Message for response: {}", response);
                os.writeUTF(response);
                os.flush();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public void getFileFromClient() throws IOException {
        String fileName = is.readUTF();
        log.debug("Client send file: {}", fileName);
        long fileSize = is.readLong();
        log.debug("Client send file size: {}", fileSize);
        File fileToTransfer = new File(serverDir.toFile(),fileName);
        FileOutputStream fos = new FileOutputStream(fileToTransfer, false);
        byte[] buffer = new byte[256];
        long byteCountTransfered = 0;
        int readsByte = 0;
        while ((readsByte = is.read(buffer)) != -1){
            fos.write(buffer,0, readsByte);
            byteCountTransfered += readsByte;
            if (byteCountTransfered == fileSize) {
                break;
            }
        }
        fos.flush();
        fos.close();
        log.debug("Client transfered {} bytes from {}.", byteCountTransfered, fileSize);
        log.debug("Client send file finished.");
        os.writeUTF("Transfer file '" + fileName + "' (" + byteCountTransfered + " b.) - complete.");
        os.flush();
    }
}
