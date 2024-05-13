package group;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Master {
  static final String[] SERVERS = { "tp-1a201-17", "tp-1a201-18", "tp-1a201-19" };
  static final int FTP_PORT = 3456;
  static final int SOCKET_PORT = 2234;

  public static void main(String[] args) {
    FTPMultiClient ftpMultiClient = new FTPMultiClient(SERVERS, FTP_PORT);
    MasterSocketClient socketServer = new MasterSocketClient(SERVERS, SOCKET_PORT);

    try {
      // socketServer.start();
      ftpMultiClient.start();
      /* Split phase */
      List<String> fileLines = Utils.readInputStream(Master.class.getResourceAsStream("/sampleText.txt"));
      List<String> filesContent = Utils.groupStringList(fileLines, SERVERS.length);

      for (int i = 0; i < SERVERS.length; i++) {
        ftpMultiClient.sendFile(i, "Split.txt",
            filesContent.get(i));
        System.out.println("Sent file to server " + i);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
