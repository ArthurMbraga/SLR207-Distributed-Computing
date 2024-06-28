package group;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class FTPMultiClient {
    String[] hosts;
    int port;

    final String USERNAME = "toto";
    final String PASSWORD = "tata";

    public FTPMultiClient(String[] hosts, int port) {
        this.hosts = hosts;
        this.port = port;
    }

    private FTPClient start(int index) throws IOException {
        FTPClient connection = new FTPClient();
        int attempts = 0;
        boolean connected = false;
        while (attempts < 3 && !connected) {
            try {
                connection.connect(hosts[index], port);
                connection.login(USERNAME, PASSWORD);
                connection.enterLocalPassiveMode();
                connection.setFileType(FTP.BINARY_FILE_TYPE);
                connected = true;
            } catch (IOException e) {
                attempts++;
                System.out.println("Connection attempt " + attempts + " failed. Retrying...");
            }
        }

        if (!connected) {
            throw new IOException("Failed to connect to FTP server after 3 attempts.");
        }

        return connection;
    }

    private void retrieveFile(String fileName, FTPClient connection) throws IOException {

        // Code to retrieve and display file content
        InputStream inputStream = connection.retrieveFileStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        reader.close();
        connection.completePendingCommand();
    }

    private final int SUCCESS_CODE = 226;

    private void createNewFile(String content, String fileName, FTPClient connection) throws IOException {
        // Create a temporary file
        File tempFile = File.createTempFile("upload", ".tmp");
        tempFile.deleteOnExit();

        // Write content to the temporary file
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        // Stream the temporary file to the FTP server
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(tempFile))) {
            connection.storeFile(fileName, inputStream);
            int replyCode = connection.getReplyCode();

            if (replyCode == SUCCESS_CODE) {
                System.out.println("File uploaded successfully.");
            } else {
                System.out.println("File upload failed. FTP Error code: " + replyCode);
            }
        } finally {
            // Ensure the temporary file is deleted
            tempFile.delete();
        }
    }

    public void sendFile(int id, String filename, String content) throws IOException {
        if (filename == null || content == null) {
            throw new IllegalArgumentException("Filename or content cannot be null");
        }

        FTPClient ftpClient = start(id);
        createNewFile(content, filename, ftpClient);
        stop(ftpClient);
    }

    public void getFile(int id, String filename) throws IOException {
        FTPClient ftpClient = start(id);
        retrieveFile(filename, ftpClient);
        stop(ftpClient);
    }

    public void stop(FTPClient connection) throws IOException {
        connection.logout();
        connection.disconnect();
    }
}
