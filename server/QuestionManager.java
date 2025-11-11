import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class QuestionManager {
    private final List<String> questions = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Consumer<String> broadcastCallback;
    private Consumer<String> questionEndCallback;
    private int currentIndex = -1;
    private int questionTimeSec = 15;

    public QuestionManager(String questionsFile) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(questionsFile));
        for (String l : lines) if (!l.trim().isEmpty()) questions.add(l.trim());
    }

    public void setOnQuestionBroadcast(Consumer<String> c) { this.broadcastCallback = c; }
    public void setOnQuestionEnd(Consumer<String> c) { this.questionEndCallback = c; }

    public void start(int perQuestionTimeSec) {
        this.questionTimeSec = perQuestionTimeSec;
        scheduler.schedule(this::nextQuestion, 0, TimeUnit.SECONDS);
    }

    private void nextQuestion() {
        currentIndex++;
        if (currentIndex >= questions.size()) {
            if (broadcastCallback != null) broadcastCallback.accept("END|done");
            return;
        }
        String line = questions.get(currentIndex);
        String[] parts = line.split("\\|", 6);
        String qid = "Q" + currentIndex;
        String out = String.format("QUESTION|%s|%s|%s|%s|%s|%s|%d", qid,
                parts[0], parts[1], parts[2], parts[3], parts[4], questionTimeSec);
        if (broadcastCallback != null) broadcastCallback.accept(out);
        scheduler.schedule(() -> {
            if (questionEndCallback != null) questionEndCallback.accept(qid);
            nextQuestion();
        }, questionTimeSec, TimeUnit.SECONDS);
    }

    public int getCorrectOption(String qid) {
        try {
            int idx = Integer.parseInt(qid.substring(1));
            String line = questions.get(idx);
            String[] parts = line.split("\\|", 6);
            return Integer.parseInt(parts[5].trim());
        } catch (Exception e) { return -1; }
    }
}
