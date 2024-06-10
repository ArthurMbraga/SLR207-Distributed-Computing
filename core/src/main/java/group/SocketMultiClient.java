package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class SocketMultiClient {
    int port;
    String[] hosts;

    Socket[] connections;

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
            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(connections[serverIndex].getOutputStream()));
            os.write(message);
            os.newLine();
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String sendMessage(int serverIndex, String message) {
        try {
            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(connections[serverIndex].getOutputStream()));
            BufferedReader is = new BufferedReader(new InputStreamReader(connections[serverIndex].getInputStream()));
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

    public CompletableFuture<String>[] sendMessage(int serverIndex, String message, int numberOfResponses) {
        try {
            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(connections[serverIndex].getOutputStream()));
            BufferedReader is = new BufferedReader(new InputStreamReader(connections[serverIndex].getInputStream()));
            os.write(message);
            os.newLine();
            os.flush();

            @SuppressWarnings("unchecked")
            CompletableFuture<String>[] responses = new CompletableFuture[numberOfResponses];

            for (int i = 0; i < numberOfResponses; i++) {
                responses[i] = CompletableFuture.supplyAsync(() -> {
                    try {
                        return is.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                });
            }

            return responses;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() throws IOException {
        for (int i = 0; i < connections.length; i++) {
            connections[i].close();
        }
    }

}