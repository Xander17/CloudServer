package app;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import resources.CommandBytes;
import settings.GlobalSettings;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(52);
            byteBuf.writeByte(CommandBytes.COMMAND_START.getByte());
            byteBuf.writeByte(CommandBytes.AUTH.getByte());
            byteBuf.writeBytes("qwerty".getBytes(), 0, 20);
            byteBuf.writeBytes("qwerty".getBytes(), 0, 30);
            out.write(byteBuf.array());
            System.out.println(in.read() == CommandBytes.COMMAND_START.getByte());
//            String[] input=in.readUTF().split(" ",2);
//            System.out.println(CommandBytes.AUTH_OK.check(input[0]));
//            getFilesList(in);

//            Set<Path> files = Files.list(REPOSITORY_DIRECTORY).collect(Collectors.toSet());
//            for (Path file : files) {
//                System.out.println("Uploading: " + file.getFileName());
//                out.write(CommandBytes.PACKAGE_START.getByte());
//                writeFileInfo(out, file);
//                writeFileData(out, file);
//                out.flush();
//            }
        } catch (ConnectException e) {
            System.out.println("Connection failed");
        } catch (IOException e) {
            e.printStackTrace();
//        } catch (NoSuchAlgorithmException e) {
//            System.out.println("Checksum calculating error");
//            e.printStackTrace();
        }
        System.out.println("Connection closed");
    }

//    private void getFilesList(DataInputStream in) throws IOException {
//        if (in.read() == CommandBytes.COMMAND_START.getByte()) ;
//        if (CommandBytes.FILELIST.check(in.readUTF())) {
//            int filesCount = in.readShort();
//            if (filesCount == 0) return;
//            for (int i = 0; i < filesCount; i++) {
//                System.out.println(getFileName(in));
//            }
//        }
//    }

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