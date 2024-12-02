import java.io.*;
import java.net.*;

public class ChatUser {
    private static Socket socket;
    private static ObjectInputStream in;

    public static void main(String[] args) {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                System.out.println("Enter server address:");
                String address = consoleReader.readLine();
                System.out.println("Enter server port:");
                int port = Integer.parseInt(consoleReader.readLine());

                socket = new Socket(address, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                System.out.println("Connected to the server.");
                new Thread(ChatUser::listenForMessages).start();

                while (true) {
                    String message = consoleReader.readLine();
                    out.writeObject(message);

                    if (message.equalsIgnoreCase("/exit")) {
                        System.out.println("You have left the chat.");
                        break;
                    }
                }
                break;
            } catch (IOException e) {
                System.err.println("Failed to connect. Retrying...");
            }
        }
    }

    private static void listenForMessages() {
        try {
            Object message;
            while ((message = in.readObject()) != null) {
                System.out.println(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Connection to server lost.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Failed to close socket: " + e.getMessage());
            }
        }
    }
}