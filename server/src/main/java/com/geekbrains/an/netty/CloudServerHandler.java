package com.geekbrains.an.netty;

import com.geekbrains.an.model.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class CloudServerHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path currentDir;
    private AuthService authService;
    private DBAuthService dbAuthService;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // init server dir
        currentDir = Paths.get("server" , "server");
        if (!Files.exists(currentDir)) {
                Files.createDirectory(currentDir);
            }
        sendList(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        switch (cloudMessage.getType()) {

            case FILE_REQUEST:
                processFileRequest((FileRequest) cloudMessage, ctx);
                break;
            case FILE:
                processFileMessage((FileMessage) cloudMessage);
                sendList(ctx);
                break;
            case IN_DIRECTORY:
                processDirectory((Directori) cloudMessage);
                sendList(ctx);
                break;
            case UP_DIRECTORY:
                btnPathUpAction();
                sendList(ctx);
                break;
            case SERVER_HOME_CLIK:
                serverHomeClik();
                sendList(ctx);
                break;
            case DELETE:
                btnDelete((DeleteFile) cloudMessage);
                sendList(ctx);
                break;
            case CREATE_FOLDER:
                createServerFolder((CreateFolder) cloudMessage);
                sendList(ctx);
                break;
            case PASTE_FILE:
                pacteFileServer((PasteFile) cloudMessage);
                sendList(ctx);
                break;
            case RENAME:
                renameFile((RenameFile) cloudMessage);
                sendList(ctx);
                break;
            case AUTH:
                log.debug("auth!!!!!!!");
                authService((User) cloudMessage);
                sendList(ctx);
                break;
            case REGISTER:
                log.debug("REG!!!!!!!");
                regServisw((User) cloudMessage);
                sendList(ctx);
                break;
        }

    }

    private void regServisw(User cloudMessage) {

        User user =  dbAuthService.regNewLoginAndPassword(cloudMessage.getLogin(), cloudMessage.getPassword());
       log.debug("Login", user.getLogin());
        if (user != null) {
            user.getTypeRegOk();
        } else {
            user.getTypeRegFail();
            log.debug("Login", user.getLogin());}
    }

    private void authService(User cloudMessage) throws IOException {
        User user = dbAuthService.findByLoginAndPassword(cloudMessage.getLogin(), cloudMessage.getPassword());
        if (user != null) {
            user.getAuthOk();
            currentDir = Paths.get("server", user.getLogin());
        if (!Files.exists(currentDir)) {
                Files.createDirectory(currentDir);
            }
        } else { user.getAuthFail();}

    }

    private void renameFile(RenameFile cloudMessage) {
        String oldName = currentDir.resolve(cloudMessage.getOldName()).toString();
        String newName = currentDir.resolve(cloudMessage.getNewName()).toString();
        File oldFile = new File(oldName);
        log.debug("oldname " + oldName);
        log.debug("newName " + newName);
                if (oldFile.renameTo(new File(newName))) {
                    log.debug("Файл переименован успешно");
                } else {
                    log.debug("Файл не был переименован");
                }
    }

    private void pacteFileServer(PasteFile cloudMessage) {
        Path path = currentDir.resolve(cloudMessage.getFileName());
        Path path_copy = currentDir.resolve("copy_" + cloudMessage.getFileName());
        Path path1 = Paths.get(cloudMessage.getPath_copy().concat("/" + cloudMessage.getFileName()));
        try {
            if (Files.exists(path)) {
                Files.copy(path1, path_copy);
                log.debug("Past file copy_" + cloudMessage.getFileName());
            } else {
                Files.copy(path1, path);
                log.debug("Past file " + cloudMessage.getFileName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createServerFolder(CreateFolder cloudMessage) {
        try {
            if (Files.exists(currentDir.resolve(cloudMessage.getFileName()))) {
                int i = 1;
                while (Files.exists(currentDir.resolve(cloudMessage.getFileName() + i))) {
                    i++;
                }
                Files.createDirectory(currentDir.resolve(cloudMessage.getFileName() + i));
                log.debug("Create directory  " + cloudMessage.getFileName() + i);
            } else {Files.createDirectory(currentDir.resolve(cloudMessage.getFileName()));}
        } catch (IOException e) {
            log.debug("Не удалось создать папку " + cloudMessage.getFileName());
            e.printStackTrace();
        }

    }

    private void btnDelete(DeleteFile fileName) throws IOException {
        Files.delete(currentDir.resolve(fileName.getFileName()));

    }

    public void serverHomeClik() {
        currentDir = Paths.get("server" , "server");
    }

    public void btnPathUpAction() {
        Path upperPath = currentDir.getParent();
        if (upperPath != null) {
            currentDir = upperPath;
        }
    }

    private void processDirectory(Directori cloudMessage) {
        Path current = currentDir.resolve(cloudMessage.getFileName());
       if (Files.isDirectory(current)) {
                        currentDir = current;
                    }
    }

    private void sendList(ChannelHandlerContext ctx) throws IOException {
        ctx.writeAndFlush(new ListMessage(currentDir));
    }

    private void processFileMessage(FileMessage cloudMessage) throws IOException {
        if (Files.exists(currentDir.resolve(cloudMessage.getFileName()))){
            if (Files.exists(currentDir.resolve("copy_" + cloudMessage.getFileName()))) {
                int i = 1;
                while (Files.exists(currentDir.resolve("copy" + i + "_" + cloudMessage.getFileName()))) {
                    i++;
                } Files.write(currentDir.resolve("copy" + i + "_" + cloudMessage.getFileName()), cloudMessage.getBytes());

            } Files.write(currentDir.resolve("copy" + "_" + cloudMessage.getFileName()), cloudMessage.getBytes());

        } else {Files.write(currentDir.resolve(cloudMessage.getFileName()), cloudMessage.getBytes());}
    }

    private void processFileRequest(FileRequest cloudMessage, ChannelHandlerContext ctx) throws IOException {
        Path path = currentDir.resolve(cloudMessage.getFileName());
        ctx.writeAndFlush(new FileMessage(path));
    }
}
