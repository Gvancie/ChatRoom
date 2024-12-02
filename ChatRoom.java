import java.io.*;
import java.net.*;
import java.util.concurrent.*;

@SuppressWarnings("InfiniteLoopStatement")
public class ChatRoom {
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static int clientCount = 0;

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        System.out.println("Enter the port number to start the server:");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            int port = Integer.parseInt(br.readLine());
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("ChatRoom server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Error in ChatRoom server: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private String name = "Anonymous";
        private ObjectOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());

                synchronized (this) {
                    clientCount++;
                    sendToAll("A new user has joined the chat. Active users: " + clientCount);
                    out.writeObject("WELCOME TO CHAT! You are logged in as 'Anonymous'.");
                }

                clients.put(name, this);

                Object message;
                while ((message = in.readObject()) != null) {
                    String msg = message.toString();

                    if (msg.startsWith("/name")) {
                        String newName = msg.split(" ", 2)[1];
                        sendToAll(name + " changed their name to " + newName);
                        clients.remove(name);
                        name = newName;
                        clients.put(name, this);
                    } else if (msg.startsWith("/pm")) {
                        String[] parts = msg.split(" ", 3);
                        String recipient = parts[1];
                        String privateMessage = parts[2];
                        sendPrivateMessage(recipient, privateMessage);
                    } else if (msg.equalsIgnoreCase("/exit")) {
                        break;
                    } else {
                        sendToAll(name + ": " + msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Connection error with client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Failed to close socket: " + e.getMessage());
                }
                synchronized (this) {
                    clients.remove(name);
                    clientCount--;
                    sendToAll(name + " has left the chat. Active users: " + clientCount);
                }
            }
        }

        private void sendToAll(String message) {
            clients.values().forEach(client -> client.sendMessage(message));
        }

        private void sendPrivateMessage(String recipient, String message) {
            ClientHandler client = clients.get(recipient);
            if (client != null) {
                client.sendMessage("Private from " + name + ": " + message);
            } else {
                sendMessage("User " + recipient + " not found.");
            }
        }

        private void sendMessage(String message) {
            try {
                out.writeObject(message);
            } catch (IOException e) {
                System.err.println("Failed to send message to " + name + ": " + e.getMessage());
            }
        }
    }
}