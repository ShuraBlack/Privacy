package de.shurablack.util;

import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class QuestionLoader {

    private static final Map<String, List<String>> SETS = new HashMap<>();

    public static void loadDecks() {
        try {
            SETS.put("spicy", fileToList("spicy.txt"));
            SETS.put("normal", fileToList("normal.txt"));
        } catch (IOException e) {
            LogManager.getLogger(QuestionLoader.class).error("Error loading decks", e);
        }
    }

    public static List<String> getSet(boolean[] set) {
        List<String> select = new ArrayList<>();
        if (set[0]) {
            select.addAll(SETS.get("spicy"));
        }
        if (set[1]) {
            select.addAll(SETS.get("normal"));
        }
        Collections.shuffle(select);
        return select;
    }

    private static List<String> fileToList(String fileName) throws IOException {
        return Files.readAllLines(Path.of("resources/questions/" + fileName));
    }
}
