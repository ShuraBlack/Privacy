package de.shurablack.websocket;

import org.java_websocket.WebSocket;

public class Connection {

    private WebSocket websocket;
    private final String uuid;
    private String nickname = "";
    private boolean host;

    // Game fields
    private int ownAnswer = -1;
    private int othersAnswer = -1;

    private int gatheredPoints;

    public Connection(WebSocket websocket, String uuid) {
        this.websocket = websocket;
        this.uuid = uuid;
        this.host = false;
    }

    public void reconnect(WebSocket websocket) {
        this.websocket = websocket;
    }

    public boolean isAlive() {
        return websocket != null && !websocket.isClosing() && !websocket.isClosed();
    }

    public void disconnect() {
        this.close(1000, "Client disconnected");
        this.websocket = null;
    }

    public WebSocket getWebsocket() {
        return websocket;
    }

    public String getUuid() {
        return uuid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public int getOwnAnswer() {
        return ownAnswer;
    }

    public void setOwnAnswer(int ownAnswer) {
        this.ownAnswer = ownAnswer;
    }

    public int getOthersAnswer() {
        return othersAnswer;
    }

    public void setOthersAnswer(int othersAnswer) {
        this.othersAnswer = othersAnswer;
    }

    public boolean hasAnswered() {
        return ownAnswer != -1 && othersAnswer != -1;
    }

    public void resetAnswers() {
        this.ownAnswer = -1;
        this.othersAnswer = -1;
    }

    public void resetAll() {
        this.ownAnswer = -1;
        this.othersAnswer = -1;
        this.gatheredPoints = 0;
    }

    public int getGatheredPoints() {
        return gatheredPoints;
    }

    public void increaseGatheredPoints() {
        this.gatheredPoints += 1;
    }

    public boolean hasWon() {
        return this.gatheredPoints == 10;
    }

    public void send(String message) {
        try {
            if (websocket.isOpen()) {
                websocket.send(message);
            }
        } catch (Exception ignored) { }
    }

    public boolean isClosed() {
        return websocket.isClosing() || websocket.isClosed();
    }

    public void close(int code, String reason) {
        websocket.close(code, reason);
    }

    public void closeConnection(int code, String reason) {
        websocket.closeConnection(code, reason);
    }
}
