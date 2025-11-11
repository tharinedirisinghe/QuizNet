import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class QuizServer {
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;

    // Maps for client state
    private final Map<SocketChannel, String> nicknames = new ConcurrentHashMap<>();
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();

    private final QuestionManager questionManager;
    private final ScoringEngine scoringEngine;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public QuizServer(int port, String questionsFile) throws IOException {
        this.port = port;
        questionManager = new QuestionManager(questionsFile);
        scoringEngine = new ScoringEngine(scores);
        init();
    }

    private void init() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started on port " + port);
    }

    public void start() throws IOException {
        questionManager.setOnQuestionBroadcast((questionLine) -> broadcast(questionLine));
        questionManager.setOnQuestionEnd((qid) -> {
            broadcast("RESULT|" + qid + "|" + questionManager.getCorrectOption(qid));
            broadcastLeaderboard(scoringEngine.getScoresSnapshot());
        });

        while (true) {
            selector.select();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                if (!key.isValid()) continue;
                if (key.isAcceptable()) handleAccept(key);
                else if (key.isReadable()) handleRead(key);
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel client = ssc.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(4096));
        write(client, "INFO|Welcome to QuizNet! Send JOIN|nickname to enter.\n");
        System.out.println("Accepted: " + client.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        try {
            int bytes = client.read(buffer);
            if (bytes == -1) {
                disconnectClient(client);
                return;
            }
            buffer.flip();
            String data = StandardCharsets.UTF_8.decode(buffer).toString();
            buffer.clear();
            String[] lines = data.split("\\r?\\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                handleClientMessage(client, line.trim());
            }
        } catch (IOException e) {
            disconnectClient(client);
        }
    }

    private void handleClientMessage(SocketChannel client, String msg) {
        System.out.println("Received from client: " + msg);
        String[] parts = msg.split("\\|", 3);
        String cmd = parts[0];
        try {
            switch (cmd) {
                case "JOIN":
                    if (parts.length < 2) { write(client, "INFO|Invalid JOIN\n"); break; }
                    String nickname = parts[1].trim();
                    nicknames.put(client, nickname);
                    scores.putIfAbsent(nickname, 0);
                    write(client, "WELCOME|session|" + nicknames.size() + "\n");
                    broadcast("INFO|" + nickname + " joined. Players: " + nicknames.size() + "\n");
                    break;
                case "ANSWER":
                    if (parts.length < 3) { write(client, "INFO|Invalid ANSWER\n"); break; }
                    String qid = parts[1];
                    String option = parts[2];
                    String nick = nicknames.get(client);
                    if (nick == null) { write(client, "INFO|You must JOIN first\n"); break; }
                    scoringEngine.submitAnswer(qid, nick, Integer.parseInt(option.trim()));
                    break;
                case "CHAT":
                    String name = nicknames.getOrDefault(client, "Anonymous");
                    String text = parts.length >= 2 ? parts[1] : "";
                    broadcast("CHAT|" + name + "|" + text + "\n");
                    break;
                case "START":
                    broadcast("INFO|Quiz will start in 3 seconds...\n");
                    scheduler.schedule(() -> questionManager.start(15), 3, TimeUnit.SECONDS);
                    break;
                default:
                    write(client, "INFO|Unknown command\n");
            }
        } catch (Exception e) {
            try { write(client, "INFO|Server error: " + e.getMessage() + "\n"); } catch (Exception ignored) {}
        }
    }

    private void broadcast(String message) {
        byte[] bytes = (message + "\n").getBytes(StandardCharsets.UTF_8);
        for (SocketChannel ch : new ArrayList<>(nicknames.keySet())) {
            try { ch.write(ByteBuffer.wrap(bytes)); } catch (IOException e) { disconnectClient(ch); }
        }
    }

    private void broadcastLeaderboard(Map<String, Integer> top) {
        StringBuilder sb = new StringBuilder();
        top.entrySet().stream()
                .sorted((a,b)->Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> sb.append(e.getKey()).append(",").append(e.getValue()).append(";"));
        broadcast("LEADERBOARD|" + sb.toString());
    }

    private void write(SocketChannel client, String msg) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
        client.write(buf);
    }

    private void disconnectClient(SocketChannel client) {
        String nick = nicknames.remove(client);
        try { client.close(); } catch (IOException ignored) {}
        if (nick != null) {
            scores.remove(nick);
            broadcast("INFO|" + nick + " disconnected\n");
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 9000;
        String qfile = "questions.txt";
        QuizServer server = new QuizServer(port, qfile);
        server.start();
    }
}
