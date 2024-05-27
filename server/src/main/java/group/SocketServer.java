package group;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private static final int PORT = 2234;
    private ExecutorService executor;
    private MessageHandler messageHandler;

    @FunctionalInterface
    interface MessageHandler {
        void onReceiveMessage(String message, BufferedWriter os) throws IOException;
    }

    public SocketServer() {
        this.executor = Executors.newFixedThreadPool(10);
    }

    public void start() {
        try (ServerSocket listener = new ServerSocket(PORT)) {
            System.out.println("Server is waiting to accept users...");

            while (true) {
                Socket socketOfServer = listener.accept();
                System.out.println("Accepted a client!");

                executor.execute(() -> {
                    try {
                        BufferedReader is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
                        BufferedWriter os = new BufferedWriter(
                                new OutputStreamWriter(socketOfServer.getOutputStream()));

                        while (true) {
                            String message = is.readLine();
                            if (message == null || "QUIT".equals(message)) {
                                break;
                            }
                            messageHandler.onReceiveMessage(message, os);
                        }
                    } catch (IOException e) {
                        System.out.println("Error handling a client: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    public void setOnReceiveMessageListener(MessageHandler listener) {
        this.messageHandler = listener::onReceiveMessage;
    }

}

// private void onReceiveMessage(String message, BufferedWriter os) {
// try {
// System.out.println("Processing message: " + message);
// if ("QUIT".equals(message)) {
// os.write(">> OK");
// os.newLine();
// os.flush();
// } else if ("MAP".equals(message)) {
// HashMap<String, Integer> hashMap = mapFunction();
// System.out.println(hashMap);
// os.write(hashMap.toString());
// os.newLine();
// os.flush();
// } else {
// os.write(">> " + message);
// os.newLine();
// os.flush();
// }
// } catch (IOException e) {
// System.out.println("Error processing message: " + e.getMessage());
// e.printStackTrace();
// }
// }