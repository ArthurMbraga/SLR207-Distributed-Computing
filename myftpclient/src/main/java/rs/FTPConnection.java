package rs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FTPConnection {
    static final String USERNAME = "toto";
    static final String PASSWORD = "tata";
    static final int PORT = 3456;
    static FTPClient ftpClient;
    static final String[] SERVERS = { "tp-1a201-17", "tp-1a201-18", "tp-1a201-19" };

    public static void main(String[] args) {

        try {
            sendSplit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendSplit() throws Exception {
        List<String> fileLines = readLocalFile("./sampleText.txt");
        List<String> messages = Utils.groupStringList(fileLines, SERVERS.length);

        for (int i = 0; i < SERVERS.length; i++) {
            String server = SERVERS[i];
            ftpClient = connectToServer(server);

            // Code to display files
            FTPFile[] files = ftpClient.listFiles();
            boolean fileExists = false;
            for (FTPFile file : files) {
                if (file.getName().equals("bonjour.txt")) {
                    fileExists = true;
                    break;
                }
            }

            if (fileExists)
                retrieveFile();
            else
                createNewFile(messages.get(i));

            ftpClient.logout();
            ftpClient.disconnect();
        }

    }

    private static FTPClient connectToServer(String server) throws SocketException, IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(server, PORT);
        ftpClient.login(USERNAME, PASSWORD);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        return ftpClient;
    }

    private static void retrieveFile() throws IOException {
        // Code to retrieve and display file content
        InputStream inputStream = ftpClient.retrieveFileStream("bonjour.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        reader.close();
        ftpClient.completePendingCommand();
    }

    private static final int SUCCESS_CODE = 226;

    private static void createNewFile(String content) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        ftpClient.storeFile("bonjour.txt", inputStream);
        int replyCode = ftpClient.getReplyCode();

        if (replyCode == SUCCESS_CODE) {
            System.out.println("File uploaded successfully.");
        } else {
            System.out.println("File upload failed. FTP Error code: " + replyCode);
        }
    }

    private static List<String> readLocalFile(String path) throws IOException {
        List<String> contentList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while ((line = reader.readLine()) != null)
            contentList.add(line);

        reader.close();
        return contentList;
    }

}
