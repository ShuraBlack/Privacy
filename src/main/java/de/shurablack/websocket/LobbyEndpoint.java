package de.shurablack.websocket;

import de.shurablack.util.GlobalTimer;
import de.shurablack.util.QuestionLoader;
import de.shurablack.util.Time;
import de.shurablack.websocket.task.InterruptibleTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LobbyEndpoint {

    private static final Logger LOGGER = LogManager.getLogger(LobbyEndpoint.class);

    private final LobbyWebSocket server;

    private final String id;
    private final List<Connection> clients = new ArrayList<>();
    private int connectedClients = 0;

    private LobbyState state = LobbyState.WAITING;

    private final boolean[] activeSets;
    private List<String> questions;
    private InterruptibleTask currentTask;
    private long timeTillNextTask = 0;

    public LobbyEndpoint(String id, boolean[] activeSets, LobbyWebSocket server) {
        this.id = id;
        this.server = server;
        this.activeSets = activeSets;
    }

    public void onOpen(WebSocket conn, String uuid) {
        if (state.equals(LobbyState.PLAYING)) {
            Connection user = getUserByUuid(uuid);
            if (user != null) {
                user.reconnect(conn);
                user.send(Response.gameValid());
                return;
            }
            conn.close(4000, "Lobby befindet sich in einer aktiven Runde");
            return;
        }

        final Connection user = getUserByUuid(uuid);
        connectedClients++;
        if (user != null && !user.isAlive()) {
            LOGGER.info("User {} reconnected to lobby {}", uuid, id);
            user.reconnect(conn);
            user.send(Response.lobbyValid());
            user.send(Response.lobbyReconnect(user.getNickname()));
            GlobalTimer.schedule(() -> joinRequest(user, new JSONObject().put("nickname", user.getNickname()), true), 1000);
            return;
        }
        LOGGER.info("User {} joined lobby {}", uuid, id);
        this.clients.add(new Connection(conn, uuid));
    }

    public void onClose(WebSocket conn, int code) {
        final Connection user = this.clients.stream()
                .filter(u -> u.getWebsocket() == conn)
                .findFirst()
                .orElse(null);
        if (user != null) {
            if (state.equals(LobbyState.PLAYING)) {
                if (code != 1001) {
                    this.clients.remove(user);
                    connectedClients--;
                }
            } else {
                user.disconnect();
                LOGGER.info("User left lobby {}", id);
                connectedClients--;
            }
        } else {
            return;
        }

        if (connectedClients == 0) {
            LOGGER.info("Lobby {} is empty, deleting", id);
            server.deleteLobby(id);
            return;
        } else {
            final String message = state.equals(LobbyState.PLAYING) ? Response.gameLeave(user.getNickname()) : Response.lobbyDisconnect(user.getNickname());
            for (Connection otherUser : this.clients) {
                if (!otherUser.isAlive()) {
                    continue;
                }
                otherUser.send(message);
            }
        }

        if (user.isHost()) {
            final Optional<Connection> nextHost = this.clients.stream().filter(u -> u.isAlive()).findFirst();
            if (nextHost.isPresent()) {
                nextHost.get().setHost(true);
                user.setHost(false);
                final String message = state.equals(LobbyState.PLAYING) ? Response.gameHostUpdate() : Response.lobbyHostUpdate(nextHost.get().getNickname());
                for (Connection otherUser : this.clients) {
                    if (!otherUser.isAlive()) {
                        continue;
                    }
                    otherUser.send(message);
                }
            } else {
                LOGGER.info("Lobby {} is empty, deleting", id);
                server.deleteLobby(id);
            }
        }
    }

    public void onMessage(WebSocket conn, String message) {
        final JSONObject jsonMessage = new JSONObject(message);
        final Connection user = getUserByUuid(jsonMessage.getString("uuid"));

        if (user == null) {
            LOGGER.warn("User {} not found in lobby {}", jsonMessage.getString("uuid"), id);
            return;
        }

        switch (jsonMessage.getString("type")) {
            case "lobby.join_request":
                final String nickname = jsonMessage.optString("nickname", user.getNickname());
                final boolean taken = this.clients.stream().anyMatch(otherUser -> otherUser.getNickname().equals(nickname) && !otherUser.getUuid().equals(user.getUuid()));
                if (taken) {
                    user.send(Response.lobbyErrorNameTaken());
                    return;
                }
                if (nickname.length() < 5 || nickname.length() > 15) {
                    user.send(Response.lobbyErrorNameLength());
                    return;
                }
                joinRequest(user, jsonMessage, false);
                break;
            case "lobby.kick":
                kickPlayer(user, jsonMessage);
                break;
            case "lobby.start":
                startGame(user);
                break;
            case "game.get_members":
                if (state.equals(LobbyState.PLAYING)) {
                    user.send(Response.gameSendMembers(clients, user.getUuid(), timeTillNextTask));
                }
                break;
            case "game.get_time":
                if (state.equals(LobbyState.PLAYING)) {
                    user.send(Response.gameTimeLeft(timeTillNextTask));
                }
                break;
            case "game.set_answer_self":
                if (state.equals(LobbyState.PLAYING)) {
                    user.setOwnAnswer(jsonMessage.getInt("value"));
                }
                if (allAnswered()) {
                    currentTask.interrupt();
                    return;
                }
                if (user.hasAnswered()) {
                    for (Connection u : this.clients) {
                        u.send(Response.gameAllAnswered(user.getNickname()));
                    }
                }
                break;
            case "game.set_answer_others":
                if (state.equals(LobbyState.PLAYING)) {
                    user.setOthersAnswer(jsonMessage.getInt("value"));
                }
                if (allAnswered()) {
                    currentTask.interrupt();
                    return;
                }
                if (user.hasAnswered()) {
                    for (Connection u : this.clients) {
                        u.send(Response.gameAllAnswered(user.getNickname()));
                    }
                }
                break;
            case "game.restart":
                if (state.equals(LobbyState.PLAYING)) {
                    restart(user);
                }
                break;
            default:
                LOGGER.warn("Unknown message type: {}", jsonMessage.getString("type"));
        }
    }

    protected Connection getUserByUuid(String uuid) {
        return clients.stream()
                .filter(user -> user.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    protected boolean isInRoom(String uuid) {
        return clients.stream()
                .anyMatch(user -> user.getUuid().equals(uuid) && user.isAlive());
    }

    private void joinRequest(Connection user, JSONObject message, boolean reconnect) {
        user.setNickname(message.getString("nickname"));
        if (this.clients.size() > 1) {
            for (Connection otherUser : this.clients) {
                if (!otherUser.getUuid().equals(user.getUuid()) && otherUser.isAlive() && !otherUser.getNickname().isBlank()) {
                    user.send(Response.lobbyJoin(otherUser.getNickname(), otherUser.isHost(), !otherUser.isAlive()));
                }
            }
        }
        user.send(Response.lobbySelfJoined(user.getNickname(), this.clients.size() == 1, activeSets));
        user.setHost(this.clients.size() == 1);
        if (this.clients.size() > 1 && !reconnect) {
            for (Connection otherUser : this.clients) {
                if (!otherUser.getUuid().equals(user.getUuid()) && otherUser.isAlive() && !otherUser.getNickname().isBlank()) {
                    otherUser.send(Response.lobbyJoin(user.getNickname(), user.isHost(), false));
                }
            }
        } else if (this.clients.size() > 1) {
            for (Connection otherUser : this.clients) {
                if (!otherUser.getUuid().equals(user.getUuid()) && otherUser.isAlive() && !otherUser.getNickname().isBlank()) {
                    otherUser.send(Response.lobbyReconnect(user.getNickname()));
                }
            }
        }
    }

    private void kickPlayer(Connection user, JSONObject message) {
        if (!user.isHost()) {
            return;
        }

        final String targetName = message.getString("nickname");
        final Connection targetUser = this.clients.stream()
                .filter(u -> u.getNickname().equals(targetName))
                .findFirst()
                .orElse(null);

        if (targetUser != null) {
            targetUser.close(1003, "Du wurdest aus der lobby gekickt!");
            this.clients.remove(targetUser);
            connectedClients--;

            for (Connection otherUser : this.clients) {
                if (!otherUser.getUuid().equals(targetUser.getUuid()) && otherUser.isAlive()) {
                    otherUser.send(Response.lobbyLeave(targetName, true));
                }
            }
        }
    }

    private void cleanDisconnected() {
        List<Connection> toRemove = new ArrayList<>();
        for (Connection u : this.clients) {
            if (!u.isAlive()) {
                toRemove.add(u);
            }
            if (u.isAlive() && u.getNickname().isBlank()) {
                toRemove.add(u);
                u.close(1003, "Du hast keinen Nickname gesetzt!");
            }
        }
        this.clients.removeAll(toRemove);
        connectedClients = this.clients.size();
        timeTillNextTask = Time.getPlusSeconds(5);

        for (Connection u : this.clients) {
            u.send(Response.gameMembersClean(toRemove, this.clients.size(), timeTillNextTask));
        }
    }

    private boolean allAnswered() {
        return this.clients.stream().allMatch(Connection::hasAnswered);
    }

    private boolean noWinner() {
        return this.clients.stream().noneMatch(Connection::hasWon);
    }

    private List<Connection> checkCorrectAnswers() {
        long sum = sumAnswers();
        List<Connection> correct = new ArrayList<>();
        for (Connection u : this.clients) {
            if (u.getOthersAnswer() == sum) {
                correct.add(u);
                u.increaseGatheredPoints();
            }
            u.resetAnswers();
        }
        return correct;
    }

    private long sumAnswers() {
        return this.clients.stream().filter(Connection::hasAnswered).filter(c -> c.getOwnAnswer() == 1).count();
    }

    private void startGame(Connection user) {
        if (!user.isHost()) {
            return;
        }

        if (state.equals(LobbyState.PLAYING)) {
            return;
        }

        state = LobbyState.PLAYING;
        questions = QuestionLoader.getSet(activeSets);
        LOGGER.info("Starting game in lobby {}", id);

        for (Connection u : this.clients) {
            if (!u.isAlive()) {
                continue;
            }
            u.send(Response.lobbyStart());
            u.disconnect();
        }

        timeTillNextTask = Time.getPlusSeconds(5);
        currentTask = getGameLoop();
        GlobalTimer.schedule(currentTask, 5000);
    }

    private void restart(Connection user) {
        if (!user.isHost()) {
            return;
        }

        if (!state.equals(LobbyState.PLAYING)) {
            return;
        }

        for (Connection u : this.clients) {
            u.send(Response.gameRestart());
            u.resetAll();
        }

        timeTillNextTask = Time.getPlusSeconds(5);
        currentTask = getGameLoop();
        GlobalTimer.schedule(currentTask, 5000);
    }

    private InterruptibleTask getGameLoop() {
        return new InterruptibleTask(() -> {
            cleanDisconnected();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) { }
            while (noWinner()) {
                timeTillNextTask = Time.getPlusSeconds(20);
                final String response = Response.gameShowQuestion(questions, timeTillNextTask);
                for (Connection u : clients) {
                    u.send(response);
                }
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException ignored) { }
                timeTillNextTask = Time.getPlusSeconds(12);
                final long sumAnswer = sumAnswers();
                final String answer = Response.gameShowAnswers(checkCorrectAnswers(), sumAnswer, timeTillNextTask);
                for (Connection u : clients) {
                    u.send(answer);
                }
                try {
                    Thread.sleep(12000);
                } catch (InterruptedException ignored) { }
            }

            for (Connection u : clients) {
                u.send(Response.gameEnd());
            }
        });
    }
}
