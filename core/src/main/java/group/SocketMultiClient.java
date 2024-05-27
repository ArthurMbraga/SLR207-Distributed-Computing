package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;

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
            os.close();
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
            os.close();
            is.close();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                connections[serverIndex].close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static HashMap<String, Integer> mapFunction() throws FileNotFoundException, IOException {
        String line;
        HashMap<String, Integer> hashMap = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader("/dev/shm/braga-23/test.txt"));

        while ((line = reader.readLine()) != null) {
            String[] words = line.split(" ");
            for (String word : words) {
                if (hashMap.containsKey(word)) {
                    hashMap.put(word, hashMap.get(word) + 1);
                } else {
                    hashMap.put(word, 1);
                }
            }
        }
        reader.close();
        return hashMap;
    }
}