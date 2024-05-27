package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class SocketServer {
    private static final int PORT = 2234;

    public void start() {
        ServerSocket listener = null;
        String line;
        BufferedReader is;
        BufferedWriter os;
        Socket socketOfServer = null;

        try {
            listener = new ServerSocket(PORT);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }

        try {
            System.out.println("Server is waiting to accept user...");

            // Accept client connection request
            // Get new Socket at Server.
            socketOfServer = listener.accept();
            System.out.println("Accept a client!");

            // Open input and output streams
            while (true) {
                is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
                os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
                // Read data to the server (sent from client).
                line = is.readLine();
                System.out.println("Read data from client: " + line);
                // Write to socket of Server
                // (Send to client)
                os.write(">> " + line);
                // End of line
                os.newLine();
                // Flush data.
                os.flush();

                // If users send QUIT (To end conversation).
                if (line == null)
                    break;
                if (line.equals("QUIT")) {
                    os.write(">> OK");
                    os.newLine();
                    os.flush();
                    break;
                }
                if (line.equals("MAP")) {
                    HashMap<String, Integer> hashMap = mapFunction();
                    System.out.println(hashMap);
                    os.write(hashMap.toString());
                    os.newLine();
                    os.flush();
                }
            }
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
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