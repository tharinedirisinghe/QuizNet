import java.io.BufferedReader;
import java.io.IOException;

public class ClientListener implements Runnable {
    private final BufferedReader reader;
    public ClientListener(BufferedReader reader) { this.reader = reader; }
    @Override
    public void run() {
        String ln;
        try {
            while ((ln = reader.readLine()) != null) {
                System.out.println("[SERVER] " + ln);
            }
        } catch (IOException e) { System.out.println("Disconnected from server."); }
    }
}
