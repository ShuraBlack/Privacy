package de.shurablack.websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LobbyWebSocket extends WebSocketServer {

    private static final Logger LOGGER = LogManager.getLogger(LobbyWebSocket.class);

    private final Map<String, LobbyEndpoint> lobbies;
    private final Map<String, Long> lastRequestTime;

    public LobbyWebSocket(InetSocketAddress address) {
        super(address);
        this.lobbies = Collections.synchronizedMap(new HashMap<>());
        this.lastRequestTime = Collections.synchronizedMap(new HashMap<>());
    }

    public void createLobby(String id, boolean[] decks) {
        LobbyEndpoint endpoint = new LobbyEndpoint(id, decks,this);
        lobbies.put("/" + id, endpoint);
    }

    public void deleteLobby(String id) {
        lobbies.remove("/" + id);
    }

    public LobbyEndpoint getLobby(String id) {
        return lobbies.get("/" + id);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String query = conn.getResourceDescriptor();
        URI uri = URI.create("wss://example.com" + query);

        String queryString = uri.getQuery();

        Map<String, String> params = Arrays.stream(queryString.split("&"))
                .map(param -> param.split("="))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));

        String uuid = params.get("uuid");

        long currentTime = System.currentTimeMillis();
        long lastRequest = this.lastRequestTime.getOrDefault(uuid, 0L);

        if (currentTime - lastRequest < 1000) {
            conn.close(1003, "Es wurden zu viele Anfragen gesendet!");
            return;
        }

        String path = URI.create(conn.getResourceDescriptor()).getPath();
        LobbyEndpoint endpoint = lobbies.get(path);
        if(endpoint != null) {
            if (endpoint.isInRoom(uuid)) {
                conn.close(1003, "Du bist bereits in diesem Raum!");
                return;
            }
            endpoint.onOpen(conn, uuid);
        } else {
            conn.close(1003, "Lobby existiert nicht!");
        }
        this.lastRequestTime.put(uuid, currentTime);
        conn.send(Response.lobbyValid());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String path = URI.create(conn.getResourceDescriptor()).getPath();
        LobbyEndpoint endpoint = lobbies.get(path);
        if(endpoint != null) {
            endpoint.onClose(conn, code);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String path = URI.create(conn.getResourceDescriptor()).getPath();
        LobbyEndpoint endpoint = lobbies.get(path);
        if(endpoint != null) {
            endpoint.onMessage(conn, message);
        } else {
            conn.closeConnection(404, "Room not found");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {

    }
}
