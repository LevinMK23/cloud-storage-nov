package com.geekbrains.an.netty;

import com.geekbrains.an.model.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
public class CloudServerHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path currentDir;
    private Path userHomeDir;

    private File authFile = new File("/home/andr/Документы/Java_4/cloud-storage-nov/server/src/main/resources/authFile.txt");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // init server dir
        IsFileExist(authFile);
        currentDir = Paths.get("server" , "server");
        if (!Files.exists(currentDir)) {
                Files.createDirectory(currentDir);
            }
        sendList(ctx);
    }

     public synchronized void IsFileExist(File file) {
        Path path = Paths.get("server", "resources");
             if (!Files.exists(path.resolve("authFile.txt"))) {
                    try {
                        Files.createFile(path.resolve("authFile.txt"));
                        log.debug("file.getName()");
                    } catch (IOException e) {
                          e.printStackTrace();
                    }
             }
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
                log.debug("AUTH!!!!!!!");
                authService(ctx, (User) cloudMessage);
                sendList(ctx);
                break;
            case REGISTER:
                log.debug("REG!!!!!!!");
                regServis(ctx, (User_reg) cloudMessage);
                sendList(ctx);
                break;
        }

    }

    public void WriteInFile(String str) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(authFile, true);
            fw.write(str + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private synchronized void regServis(ChannelHandlerContext ctx, User_reg cloudMessage) {
        if (!isUserExist(cloudMessage.getLogin())) {
            WriteInFile( "un" + cloudMessage.getLogin());
            WriteInFile("pw" + cloudMessage.getPassword());
            ctx.writeAndFlush(new User_reg(cloudMessage.getLogin(), cloudMessage.getPassword()));
        } else {
            ctx.writeAndFlush(new User_reg_fail());
            log.debug("Reg fail");
        }
    }

    private synchronized boolean isUserExist(String userLogin) {
        try {
            FileInputStream fis = new FileInputStream(authFile);
            InputStreamReader isr = new InputStreamReader(fis, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while ((line = br.readLine()) != null) {
                if (line.equals("un" + userLogin)) {
                    return true;
                }
            }
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private synchronized void authService(ChannelHandlerContext ctx, User cloudMessage)  {
        log.debug("authServis started");
        try {
            FileInputStream fis = new FileInputStream(authFile);
            InputStreamReader isr = new InputStreamReader(fis, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            boolean flag = false;
            while ((line = br.readLine()) != null) {
                log.debug("поиск логина", line);
                if (line.equals("un" + cloudMessage.getLogin())) {
                    if (br.readLine().equals("pw" + cloudMessage.getPassword())) {
                        ctx.writeAndFlush(cloudMessage);
                        userHomeDir = Paths.get("server", cloudMessage.getLogin());
                        currentDir = userHomeDir;
                        log.debug("new currentDir " + currentDir.toAbsolutePath().normalize());
                        if (!Files.exists(currentDir)) {
                            Files.createDirectory(currentDir);
                        }
                        flag = true;
                    }
                }
            }
            if (flag == false) {
                ctx.writeAndFlush(new User_fail());
            }
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        currentDir = userHomeDir;
    }

    public void btnPathUpAction() {
        Path upperPath = currentDir.getParent();
        if (upperPath != null && currentDir.equals(userHomeDir)) {
            currentDir = userHomeDir;
        } else {currentDir = upperPath;}
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
