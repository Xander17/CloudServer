import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8189);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("Connected to server.");
            Set<Path> files = Files.list(Paths.get("client-root")).collect(Collectors.toSet());
            for (Path file : files) {
                out.write(PackageSettings.PACKAGE_START);
                writeFileInfo(out, file);
                writeFileData(out, file);
                out.flush();
            }
        } catch (ConnectException e) {
            System.out.println("Connection failed");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Connection closed");
    }

    private static void writeFileInfo(DataOutputStream out, Path file) throws IOException {
        String f = file.getFileName().toString();
        out.write((byte) f.length());
        out.write(f.getBytes());
    }

    private static void writeFileData(DataOutputStream out, Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}