package group;

import java.io.*;
import java.net.*;

public class MasterSocketClient {
    int port;
    String[] hosts;

    Socket[] connections;
    BufferedWriter os = null;
    BufferedReader is = null;

    public MasterSocketClient(String[] hosts, int port) {
        this.hosts = hosts;
        this.port = port;
    }

    public void start() throws IOException {
        connections = new Socket[hosts.length];
        for (int i = 0; i < hosts.length; i++)
            connections[i] = new Socket(hosts[i], port);
    }

    public void sendMessageToEveryone(String message) {
        for (int i = 0; i < connections.length; i++) {
            try {
                os = new BufferedWriter(new OutputStreamWriter(connections[i].getOutputStream()));
                is = new BufferedReader(new InputStreamReader(connections[i].getInputStream()));
                os.write(message);
                os.newLine();
                os.flush();
                os.close();
                is.close();
                connections[i].close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}