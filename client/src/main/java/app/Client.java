package app;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {


    public static void main(String[] args) {
        new Client();
    }

    private final String HOST = "localhost";
    private final int BUFFER_SIZE = 8192;
    private final Path REPOSITORY_DIRECTORY = Paths.get("client-repo");

    public Client() {

//            Set<Path> files = Files.list(REPOSITORY_DIRECTORY).collect(Collectors.toSet());
//            for (Path file : files) {
//                System.out.println("Uploading: " + file.getFileName());
//                out.write(CommandBytes.PACKAGE_START.getByte());
//                writeFileInfo(out, file);
//                writeFileData(out, file);
//                out.flush();
//            }

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

//    private String getFileName(DataInputStream in) throws IOException {
//        int length = in.readShort();
//        byte[] bytes = new byte[length];
//        if (in.read(bytes) != length) return null;
//        return new String(bytes);
//    }

    }
}