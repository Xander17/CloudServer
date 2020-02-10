package app;

import resources.CommandMessage;
import settings.GlobalSettings;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client {


    public static void main(String[] args) {
        new Client();
    }

    private final String HOST = "localhost";
    private final int BUFFER_SIZE = 8192;
    private final Path REPOSITORY_DIRECTORY = Paths.get("client-repo");

    public Client() {
        try (Socket socket = new Socket(HOST, GlobalSettings.CONNECTION_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            if (Files.notExists(REPOSITORY_DIRECTORY)) Files.createDirectory(REPOSITORY_DIRECTORY);
            System.out.println("Connected to server.");

//            out.write(GlobalSettings.COMMAND_START_SIGNAL_BYTE);
//            out.writeUTF("reg qwerty2 qwerty2");

            out.write(GlobalSettings.COMMAND_START_SIGNAL_BYTE);
            out.writeUTF("auth qwerty2 qwerty2");
            System.out.println(in.read()==GlobalSettings.COMMAND_START_SIGNAL_BYTE);
            String[] input=in.readUTF().split(" ",2);
            System.out.println(CommandMessage.AUTH_OK.check(input[0]));
            getFilesList(in);

            Set<Path> files = Files.list(REPOSITORY_DIRECTORY).collect(Collectors.toSet());
            for (Path file : files) {
                System.out.println("Uploading: " + file.getFileName());
                out.write(GlobalSettings.PACKAGE_START_SIGNAL_BYTE);
                writeFileInfo(out, file);
                writeFileData(out, file);
                out.flush();
            }
        } catch (ConnectException e) {
            System.out.println("Connection failed");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Checksum calculating error");
            e.printStackTrace();
        }
        System.out.println("Connection closed");
    }

    private void getFilesList(DataInputStream in) throws IOException {
        if (in.read() == GlobalSettings.COMMAND_START_SIGNAL_BYTE) ;
        if (CommandMessage.FILELIST.check(in.readUTF())) {
            int filesCount = in.readShort();
            if (filesCount == 0) return;
            for (int i = 0; i < filesCount; i++) {
                System.out.println(getFileName(in));
            }
        }
    }

    private String getFileName(DataInputStream in) throws IOException {
        int length = in.readShort();
        byte[] bytes = new byte[length];
        if (in.read(bytes) != length) return null;
        return new String(bytes);
    }

    private void writeFileInfo(DataOutputStream out, Path file) throws IOException {
        String f = file.getFileName().toString();
        out.writeShort((short) f.length());
        out.write(f.getBytes());
    }

    private void writeFileData(DataOutputStream out, Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(GlobalSettings.CHECKSUM_PROTOCOL);
        byte[] bytes = new byte[BUFFER_SIZE];
        out.writeLong(Files.size(file));
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
        int n;
        while ((n = in.read(bytes)) != -1) {
            out.write(bytes, 0, n);
            md.update(bytes, 0, n);
        }
        out.write(md.digest());
        in.close();
    }
}