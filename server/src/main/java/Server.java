import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Server starts.");
            try (Socket socket = serverSocket.accept();
                 BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
                System.out.println("Client connected.");
                int b;
                while ((b = in.read()) != -1) {
                    if (!checkPackageStart(b)) continue;
                    String name = getFileName(in);
                    if (name == null) continue;
                    System.out.println("Downloading: " + name);
                    byte[] bytes = getData(in);
                    if (bytes.length == 0) continue;
                    FileOutputStream out = new FileOutputStream("server-root/" + name);
                    out.write(bytes);
                    out.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server shutdown.");
    }

    private static boolean checkPackageStart(int b) {
        return b == PackageSettings.PACKAGE_START;
    }

    private static String getFileName(BufferedInputStream in) throws IOException {
        int b = in.read();
        if (b == -1) return null;
        int length = b;
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < length; i++) {
            b = in.read();
            if (b == -1) return null;
            name.append((char) b);
        }
        return name.toString();
    }

    private static byte[] getData(BufferedInputStream in) throws IOException {
        while (in.available() < 4) ;
        byte[] bytes = new byte[4];
        in.read(bytes);
        int length = ByteBuffer.wrap(bytes).getInt();
        System.out.println(length);
        if (length <= 0) return new byte[0];
        bytes = new byte[length];
        int data;
        for (int i = 0; i < length; i++) {
            while ((data = in.read()) == -1) ;
            bytes[i] = (byte) data;
        }
        return bytes;
    }
}
