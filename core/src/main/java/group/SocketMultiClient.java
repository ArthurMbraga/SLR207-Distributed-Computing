package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SocketMultiClient {
    int port;
    String[] hosts;

    Socket[] connections;
    BufferedWriter os = null;
    BufferedReader is = null;

    public SocketMultiClient(String[] hosts, int port) {
        this.hosts = hosts;
        this.port = port;
    }

    public void start() throws IOException {
        connections = new Socket[hosts.length];
        for (int i = 0; i < hosts.length; i++)
            connections[i] = new Socket(hosts[i], port);
    }

    public void sendMessageAsync(int serverIndex, String message) {
        try {
            os = new BufferedWriter(new OutputStreamWriter(connections[serverIndex].getOutputStream()));
            os.write(message);
            os.newLine();
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String sendMessage(int serverIndex, String message) {
        try {
            os = new BufferedWriter(new OutputStreamWriter(connections[serverIndex].getOutputStream()));
            is = new BufferedReader(new InputStreamReader(connections[serverIndex].getInputStream()));
            os.write(message);
            os.newLine();
            os.flush();
            String response = is.readLine();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}