import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoringEngine {
    private final Map<String, Integer> scores;
    private final Map<String, Set<String>> answered = new ConcurrentHashMap<>();

    public ScoringEngine(Map<String, Integer> scores) {
        this.scores = scores;
    }

    public void submitAnswer(String qid, String nickname, int answerOption) {
        answered.putIfAbsent(qid, ConcurrentHashMap.newKeySet());
        Set<String> s = answered.get(qid);
        synchronized (s) {
            if (s.contains(nickname)) return;
            s.add(nickname);
        }
        // Award 1 point immediately (simple scoring)
        scores.put(nickname, scores.getOrDefault(nickname, 0) + 1);
    }

    public Map<String, Integer> getScoresSnapshot() {
        return new HashMap<>(scores);
    }
}
