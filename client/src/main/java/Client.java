import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.stream.Collectors;

public class Client {

    private final String HOST = "localhost";
    private final int BUFFER_SIZE = 8192;

    public Client() {
        try (Socket socket = new Socket(HOST, GlobalSettings.CONNECTION_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("Connected to server.");
            Set<Path> files = Files.list(Paths.get("client-root")).collect(Collectors.toSet());
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