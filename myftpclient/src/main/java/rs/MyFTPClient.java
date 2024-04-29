package rs;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;

public class MyFTPClient {
    static final String USERNAME = "toto";
    static final String PASSWORD = "tata";
    static final int PORT = 3456;
    static FTPClient ftpClient;

    public static void main(String[] args) {
        String[] servers = { "tp-1a201-17", "tp-1a201-18", "tp-1a201-19" };
        String[] messages = { "dog cat hello", "hello world cat", "cat dog hi" };

        try {
            for (int i = 0; i < servers.length; i++) {
                String server = servers[i];
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
                    createNewFile(messages[i]);

                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
}
