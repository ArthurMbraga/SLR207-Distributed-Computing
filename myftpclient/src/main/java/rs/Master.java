package rs;

import java.util.List;

public class Master {
  static final String[] SERVERS = { "tp-1a201-17", "tp-1a201-18", "tp-1a201-19" };
  static final int FTP_PORT = 4456;
  static final int SOCKET_PORT = 2234;

  public static void main(String[] args) {
    MasterFTPClient ftpServer = new MasterFTPClient(SERVERS, FTP_PORT);
    MasterSocketClient socketServer = new MasterSocketClient(SERVERS, SOCKET_PORT);

    try {
      socketServer.start();
      ftpServer.start();

      /* Split phase */
      List<String> fileLines = Utils.readLocalFile("./sampleText.txt");
      List<String> filesContent = Utils.groupStringList(fileLines, SERVERS.length);

      for (int i = 0; i < SERVERS.length; i++) {
        ftpServer.sendFile(i, "Split.txt",
            filesContent.get(i));
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
