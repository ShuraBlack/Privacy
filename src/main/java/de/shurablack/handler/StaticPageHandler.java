package de.shurablack.handler;

import de.shurablack.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class StaticPageHandler extends RequestHandler {

    private static final Logger LOGGER = LogManager.getLogger(StaticPageHandler.class);

    private final PageStore pageStore;

    private final RequestBlocker requestBlocker;

    public StaticPageHandler(PageStore pageStore) {
        this.pageStore = pageStore;
        this.requestBlocker = new RequestBlocker(List.of(
                BlockedPath.of(Method.POST,         "/dns-query",           String::equals),
                BlockedPath.of(Method.POST,         "/ajax",                String::equals),
                BlockedPath.of(Method.POST,         "/vpnsvc/connect.cgi",  String::equals),
                BlockedPath.of(Method.POST,         "/wsman",               String::equals),
                BlockedPath.of(Method.POST,         "/cgi-bin/",            String::startsWith),
                BlockedPath.of(Method.POST,         "/",                    String::equals),
                BlockedPath.of(Method.UNSUPPORTED,  "/",                    String::equals)
        ));
    }

    @Override
    public void get(Request request) {
        request.addResponseHeader("Access-Control-Allow-Origin", "*");
        request.addResponseHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        request.addResponseHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        if (request.getPath().equals("/robots.txt")) {
            LOGGER.info("GET request for {}", request.getPath());
            request.sendResponseWithClose(200, "User-agent: *\n" +
                    "Disallow: /lobby\n" +
                    "Disallow: /game\n", ContentType.TXT);
            return;
        }
        request.sendResponseWithClose(200, pageStore.getPage(request.getPath()).getBytes(), PageStore.DEFAULT_CONTENT_TYPE);
    }

    @Override
    public void post(Request request) {
        if (requestBlocker.blockedPath(request)) {
            return;
        }
        super.post(request);
    }

    @Override
    public void put(Request request) {
        if (requestBlocker.blockedPath(request)) {
            return;
        }
        super.put(request);
    }

    @Override
    public void delete(Request request) {
        if (requestBlocker.blockedPath(request)) {
            return;
        }
        super.delete(request);
    }

    @Override
    public void options(Request request) {
        request.addResponseHeader("Access-Control-Allow-Origin", "*");
        request.addResponseHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        request.addResponseHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        request.sendEmptyResponse(200);
    }

    @Override
    public void unsupported(Request request) {
        if (requestBlocker.blockedPath(request)) {
            return;
        }
        super.unsupported(request);
    }
}
