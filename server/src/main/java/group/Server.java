package group;

public class Server {

  static FTPServer ftpServer;

  static SocketServer socketServer;

  public static void main(String[] args) {
    Logger.configure();
    ftpServer = new FTPServer();
    socketServer = new SocketServer();
    socketServer.start();
  }
}
