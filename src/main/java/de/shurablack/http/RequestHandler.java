package de.shurablack.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.shurablack.util.BlockedRequestLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RequestHandler implements HttpHandler {

    private static final Logger LOGGER = LogManager.getLogger(RequestHandler.class);

    public static final Map<String, Long> BLACKLIST = new ConcurrentHashMap<>();

    @Override
    public void handle(HttpExchange exchange) {
        Request request = new Request(exchange);

        if (BLACKLIST.containsKey(exchange.getRemoteAddress().getAddress().toString())) {
            BlockedRequestLogger.log(request);
            exchange.close();
            return;
        }

        switch (request.getMethod()) {
            case GET:         get(request);         break;
            case POST:        post(request);        break;
            case PUT:         put(request);         break;
            case DELETE:      delete(request);      break;
            case HEAD:        head(request);        break;
            case OPTIONS:     options(request);     break;
            case UNSUPPORTED: unsupported(request); break;
        }
    }

    public void get(Request request) {
        logInvalidRequest(request);
        request.sendEmptyResponse(405);
    }

    public void post(Request request) {
        logInvalidRequest(request);
        request.sendEmptyResponse(405);
    }

    public void put(Request request) {
        logInvalidRequest(request);
        request.sendEmptyResponse(405);
    }

    public void delete(Request request) {
        logInvalidRequest(request);
        request.sendEmptyResponse(405);
    }

    public void head(Request request) {
        logInvalidRequest(request);
        request.sendEmptyResponse(405);
    }

    public void options(Request request) {
        logInvalidRequest(request);
        request.sendEmptyResponse(200);
    }

    public void unsupported(Request request) {
        logInvalidRequest(request);
        request.sendEmptyResponse(405);
    }

    protected void logInvalidRequest(Request request) {
        LOGGER.info("<IP:{}> {} Invalid request to {}",
                request.getOrigin().getRemoteAddress().getAddress(),
                request.getMethod(), request.getOrigin().getRequestURI()
        );
        BLACKLIST.put(request.getOrigin().getRemoteAddress().getAddress().toString(), System.currentTimeMillis());
    }
}
