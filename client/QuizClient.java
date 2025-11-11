import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class QuizClient {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public QuizClient(String host, int port) {
        this.host = host; this.port = port;
    }

    public void start() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        // Thread to listen server
        new Thread(new ClientListener(reader)).start();

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your nickname: ");
        String nick = sc.nextLine().trim();
        send("JOIN|" + nick);

        while (true) {
            String line = sc.nextLine();
            if (line.equalsIgnoreCase("quit")) break;
            if (line.startsWith("answer ")) {
                String[] parts = line.split(" ");
                if (parts.length >= 3) send("ANSWER|" + parts[1] + "|" + parts[2]);
            } else if (line.startsWith("chat ")) {
                send("CHAT|" + line.substring(5));
            } else if (line.equalsIgnoreCase("start")) {
                send("START|");
            } else {
                System.out.println("Commands: 'answer Qid opt', 'chat message', 'start', 'quit'");
            }
        }
        socket.close();
    }

    private void send(String text) throws IOException {
        writer.write(text + "\n");
        writer.flush();
    }

    public static void main(String[] args) throws IOException {
        QuizClient client = new QuizClient("localhost", 9000);
        client.start();
    }
}
