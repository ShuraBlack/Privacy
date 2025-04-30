package de.shurablack.handler;

import de.shurablack.http.Request;
import de.shurablack.http.RequestHandler;
import org.json.JSONObject;
import de.shurablack.Server;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class GameCreateHandler extends RequestHandler {

    private final Server server;
    private final ConcurrentHashMap<String, Long> lastRequestTime;

    public GameCreateHandler(final Server server) {
        this.server = server;
        this.lastRequestTime = new ConcurrentHashMap<>();
    }

    private boolean checkRateLimit(final Request request, JSONObject body) {
        long currentTime = System.currentTimeMillis();
        long lastRequest = this.lastRequestTime.getOrDefault(body.getString("uuid"), 0L);

        if (currentTime - lastRequest < 300000) {
            final JSONObject response = new JSONObject();
            response.put("statusCode", 429);
            response.put("appendix", "Zu viele Anfragen in kurzer Zeit");
            response.put("identifier", "");
            request.sendResponseWithClose(200, response);
            return true;
        }

        this.lastRequestTime.put(body.getString("uuid"), currentTime);
        return false;
    }

    @Override
    public void post(Request request) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(3000, 5000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final Optional<JSONObject> body = request.bodyAsJson();
        if (body.isEmpty()) {
            final JSONObject response = new JSONObject();
            response.put("statusCode", 400);
            response.put("appendix", "Anfrage ist fehlerhaft");
            response.put("identifier", "");
            request.sendResponseWithClose(200, response);
            return;
        }

        final JSONObject bodyJson = body.get();

        if (!bodyJson.has("uuid")) {
            final JSONObject response = new JSONObject();
            response.put("statusCode", 401);
            response.put("appendix", "Anfrage enthält keine authentifizierbare UUID");
            response.put("identifier", "");
            request.sendResponseWithClose(200, response);
            return;
        }

        if (this.checkRateLimit(request, bodyJson)) {
            return;
        }

        final String uuid = bodyJson.getString("uuid");
        final JSONObject decks = bodyJson.getJSONObject("decks");
        boolean[] usedDecks = {decks.getBoolean("spicy"), decks.getBoolean("regular")};

        final String code = this.server.getAvailableCode();
        this.server.createLobby(code, usedDecks, uuid);

        final JSONObject response = new JSONObject();
        response.put("statusCode", 200);
        response.put("identifier", code);
        response.put("appendix","");

        request.sendResponseWithClose(200, response);
    }


}
