package group;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class MasterFTPClient {
    String[] hosts;
    int port;

    FTPClient[] connections;

    final String USERNAME = "toto";
    final String PASSWORD = "tata";

    public MasterFTPClient(String[] hosts, int port) {
        this.hosts = hosts;
        this.port = port;
    }

    public void start() throws IOException {
        connections = new FTPClient[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            connections[i] = new FTPClient();
            connections[i].connect(hosts[i], port);
            connections[i].login(USERNAME, PASSWORD);
            connections[i].enterLocalPassiveMode();
            connections[i].setFileType(FTP.BINARY_FILE_TYPE);
        }
    }

    // public void sendSplit() throws Exception {
    // List<String> fileLines = readLocalFile("./sampleText.txt");
    // List<String> messages = Utils.groupStringList(fileLines, SERVERS.length);

    // for (int i = 0; i < SERVERS.length; i++) {
    // String server = SERVERS[i];
    // ftpClient = connectToServer(server);

    // // Code to display files
    // FTPFile[] files = ftpClient.listFiles();
    // boolean fileExists = false;
    // for (FTPFile file : files) {
    // if (file.getName().equals("bonjour.txt")) {
    // fileExists = true;
    // break;
    // }
    // }

    // if (fileExists)
    // retrieveFile();
    // else
    // createNewFile(messages.get(i));

    // }

    // }

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
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        connection.storeFile(fileName, inputStream);
        int replyCode = connection.getReplyCode();

        if (replyCode == SUCCESS_CODE) {
            System.out.println("File uploaded successfully.");
        } else {
            System.out.println("File upload failed. FTP Error code: " + replyCode);
        }
    }

    public void sendFile(int id, String filename, String content) throws IOException {
        FTPClient ftpClient = connections[id];
        createNewFile(content, filename, ftpClient);
    }

    public void getFile(int id, String filename) throws IOException {
        FTPClient ftpClient = connections[id];
        retrieveFile(filename, ftpClient);
    }

    public void close() throws IOException {
        for (FTPClient connection : connections) {
            connection.logout();
            connection.disconnect();
        }
    }
}
