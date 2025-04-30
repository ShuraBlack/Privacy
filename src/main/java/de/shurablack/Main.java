package de.shurablack;

import de.shurablack.util.QuestionLoader;

public class Main {
    public static void main(String[] args) {
        Server.startScheduler();
        QuestionLoader.loadDecks();
        new Server().start();
    }
}
