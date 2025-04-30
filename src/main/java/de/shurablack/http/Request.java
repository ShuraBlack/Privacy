package de.shurablack.http;

import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Request {

    private static final Logger LOGGER = LogManager.getLogger(Request.class);

    private final HttpExchange origin;
    private final Method method;
    private Map<String, String> query;

    public Request(final HttpExchange origin) {
        this.origin = origin;
        this.method = Method.fromString(origin.getRequestMethod());
        parseQuery();
    }

    private void parseQuery() {
        String baseQuery = origin.getRequestURI().getQuery();
        if (baseQuery == null) {
            return;
        }

        this.query = new HashMap<>();
        String[] requestQuery = origin.getRequestURI().getQuery().split("&");
        for (String q : requestQuery) {
            String[] pair = q.split("=");

            if (pair.length == 1) {
                this.query.put(pair[0], "");
            } else {
                this.query.put(pair[0], pair[1]);
            }
        }
    }

    public HttpExchange getOrigin() {
        return origin;
    }

    public Method getMethod() {
        return method;
    }

    public Optional<String> bodyAsString() {
        try {
            return Optional.of(new String(origin.getRequestBody().readAllBytes()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<JSONObject> bodyAsJson() {
        try {
            return Optional.of(new JSONObject(new String(origin.getRequestBody().readAllBytes())));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean queryContains(final String key) {
        return query.containsKey(key);
    }

    public String getQuery(final String key) {
        return query.get(key);
    }

    public boolean hasQuery() {
        return query != null;
    }

    public String getPath() {
        return origin.getRequestURI().getPath();
    }

    public String getHeader(final String key) {
        return origin.getRequestHeaders().getFirst(key);
    }

    public void addResponseHeader(final String key, final String value) {
        origin.getResponseHeaders().add(key, value);
    }

    public void sendResponseWithClose(final int code, final byte[] data, final ContentType contentType) {
        origin.getResponseHeaders().set("Content-Type", contentType.toString());
        try {
            origin.sendResponseHeaders(code, data.length);
            origin.getResponseBody().write(data);
        } catch (Exception e) {
            LOGGER.error("Error while sending response");
        } finally {
            origin.close();
        }
    }

    public void sendResponseWithClose(final int code, final String data, final ContentType contentType) {
        origin.getResponseHeaders().set("Content-Type", contentType.toString());
        try {
            origin.sendResponseHeaders(code, data.length());
            origin.getResponseBody().write(data.getBytes());
            origin.close();
        } catch (Exception e) {
            LOGGER.error("Error while sending response", e);
        } finally {
            origin.close();
        }
    }

    public void sendResponseWithClose(final int code, final JSONObject data) {
        String dataString = data.toString();
        origin.getResponseHeaders().set("Content-Type", ContentType.JSON.toString());
        try {
            origin.sendResponseHeaders(code, dataString.length());
            origin.getResponseBody().write(dataString.getBytes());
            origin.close();
        } catch (Exception e) {
            LOGGER.error("Error while sending response");
        } finally {
            origin.close();
        }
    }

    public void sendResponse(final int code, final byte[] data, final ContentType contentType) {
        origin.getResponseHeaders().set("Content-Type", contentType.toString());
        try {
            origin.sendResponseHeaders(code, data.length);
            origin.getResponseBody().write(data);
        } catch (Exception e) {
            LOGGER.error("Error while sending response");
            origin.close();
        }
    }

    public void sendResponse(final int code, final String data, final ContentType contentType) {
        origin.getResponseHeaders().set("Content-Type", contentType.toString());
        try {
            origin.sendResponseHeaders(code, data.length());
            origin.getResponseBody().write(data.getBytes());
        } catch (Exception e) {
            LOGGER.error("Error while sending response");
            origin.close();
        }
    }

    public void sendResponse(final int code, final JSONObject data, final ContentType contentType) {
        String dataString = data.toString();
        origin.getResponseHeaders().set("Content-Type", contentType.toString());
        try {
            origin.sendResponseHeaders(code, dataString.length());
            origin.getResponseBody().write(dataString.getBytes());
        } catch (Exception e) {
            LOGGER.error("Error while sending response", e);
            origin.close();
        }
    }

    public void sendEmptyResponse(final int code) {
        try {
            origin.sendResponseHeaders(code, -1);
        } catch (Exception e) {
            LOGGER.error("Error while sending response", e);
        } finally {
            origin.close();
        }

    }
}
