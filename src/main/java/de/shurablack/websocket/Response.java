package de.shurablack.websocket;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class Response {

    public static String lobbyValid() {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.valid");
        return response.toString();
    }

    public static String lobbySelfJoined(String name, boolean isHost, boolean[] decks) {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.accepted_join");
        response.put("name", name);
        response.put("isHost", isHost);
        response.put("decks", decks);
        return response.toString();
    }

    public static String lobbyJoin(String name, boolean isHost, boolean disconnected) {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.user_join");
        response.put("name", name);
        response.put("isHost", isHost);
        response.put("disconnected", disconnected);
        return response.toString();
    }

    public static String lobbyLeave(String name, boolean kicked) {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.user_leave");
        response.put("kicked", kicked);
        response.put("name", name);
        return response.toString();
    }

    public static String lobbyHostUpdate(String name) {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.host_update");
        response.put("name", name);
        return response.toString();
    }

    public static String gameHostUpdate() {
        JSONObject response = new JSONObject();
        response.put("type", "game.host_update");
        return response.toString();
    }

    public static String lobbyErrorNameTaken() {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.name_taken");
        return response.toString();
    }

    public static String lobbyErrorNameLength() {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.name_length");
        return response.toString();
    }

    public static String lobbyReconnect(String name) {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.reconnect");
        response.put("name", name);
        return response.toString();
    }

    public static String lobbyDisconnect(String name) {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.disconnect");
        response.put("name", name);
        return response.toString();
    }

    public static String lobbyStart() {
        JSONObject response = new JSONObject();
        response.put("type", "lobby.start");
        return response.toString();
    }

    public static String gameValid() {
        JSONObject response = new JSONObject();
        response.put("type", "game.valid");
        return response.toString();
    }

    public static String gameTimeLeft(long timeTillNext) {
        JSONObject response = new JSONObject();
        response.put("type", "game.time_left");
        response.put("timeTillNext", timeTillNext);
        return response.toString();
    }

    public static String gameSendMembers(List<Connection> clients, String uuid, long timeTillNext) {
        JSONObject response = new JSONObject();
        response.put("type", "game.members");
        JSONArray members = new JSONArray();
        for (Connection client : clients) {
            if (client.getUuid().equals(uuid)) {
                response.put("isHost", client.isHost());
                response.put("nickname", client.getNickname());
                continue;
            }
            members.put(client.getNickname());
        }
        response.put("members", members);
        response.put("timeTillNext", timeTillNext);
        return response.toString();
    }

    public static String gameLeave(String nickname) {
        JSONObject response = new JSONObject();
        response.put("type", "game.leave");
        response.put("nickname", nickname);
        return response.toString();
    }

    public static String gameMembersClean(List<Connection> clients, int maxPlayerCount, long timeTillNext) {
        JSONObject response = new JSONObject();
        response.put("type", "game.members_clean");
        JSONArray members = new JSONArray();
        for (Connection client : clients) {
            members.put(client.getNickname());
        }
        response.put("members", members);
        response.put("maxPlayerCount", maxPlayerCount);
        response.put("timeTillNext", timeTillNext);
        return response.toString();
    }

    public static String gameShowQuestion(List<String> questions, long timeTillNext) {
        final String question = questions.remove(0);
        JSONObject response = new JSONObject();
        response.put("type", "game.show_question");
        response.put("question", question);
        response.put("timeTillNext", timeTillNext);
        return response.toString();
    }

    public static String gameAllAnswered(String nickname) {
        JSONObject response = new JSONObject();
        response.put("type", "game.all_answered");
        response.put("nickname", nickname);
        return response.toString();
    }

    public static String gameShowAnswers(List<Connection> correct, long answerSum, long timeTillNext) {
        JSONObject response = new JSONObject();
        response.put("type", "game.show_answers");
        JSONArray members = new JSONArray();
        for (Connection client : correct) {
            members.put(client.getNickname());
        }
        response.put("answerSum", answerSum);
        response.put("members", members);
        response.put("timeTillNext", timeTillNext);
        return response.toString();
    }

    public static String gameEnd() {
        JSONObject response = new JSONObject();
        response.put("type", "game.end");
        return response.toString();
    }

    public static String gameRestart() {
        JSONObject response = new JSONObject();
        response.put("type", "game.restart");
        return response.toString();
    }

}
