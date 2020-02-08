import services.LogService;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Server {

    private final int BUFFER_SIZE = 8192;

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(GlobalSettings.CONNECTION_PORT)) {
            LogService.SERVER.info("Server starts.");
            try (Socket socket = serverSocket.accept();
                 DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
                System.out.println("Client connected.");
                int b;
                while ((b = in.read()) != -1) {
                    if (!checkPackageStart(b)) continue;
                    String name = getFileName(in);
                    if (name == null) continue;
                    System.out.println("Downloading: " + name);
                    if (downloadFileData(in, name)) System.out.println("Download complete");
                    else System.out.println("Download failed");
                }
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Checksum calculating error");
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server shutdown.");
    }

    private boolean checkPackageStart(int b) {
        return b == GlobalSettings.PACKAGE_START_SIGNAL_BYTE;
    }

    private String getFileName(DataInputStream in) throws IOException {
        int length = in.readShort();
        byte[] bytes = new byte[length];
        if (in.read(bytes) != length) return null;
        return new String(bytes);
    }

    private boolean downloadFileData(DataInputStream in, String filename) throws IOException, NoSuchAlgorithmException {
        long length = in.readLong();
        if (length <= 0) return false;
        byte[] bytes = new byte[BUFFER_SIZE];
        MessageDigest md = MessageDigest.getInstance(GlobalSettings.CHECKSUM_PROTOCOL);
        FileOutputStream out = new FileOutputStream("server-root/" + filename);
        while (length > 0) {
            int blockSize = length >= BUFFER_SIZE ? BUFFER_SIZE : (int) length;
            int bytesRead = in.read(bytes, 0, blockSize);
            out.write(bytes, 0, bytesRead);
            md.update(bytes, 0, bytesRead);
            length -= bytesRead;
        }
        return equalCheckSum(in, md.digest());
    }

    private boolean equalCheckSum(DataInputStream in, byte[] downloaded) throws IOException {
        for (int i = 0; i < GlobalSettings.CHECKSUM_LENGTH; i++) {
            if ((byte) in.read() != downloaded[i]) {
                System.out.println("Checksum error");
                return false;
            }
        }
        System.out.println("Checksum OK");
        return true;
    }
}
