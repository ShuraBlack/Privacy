package de.shurablack;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import de.shurablack.handler.GameCreateHandler;
import de.shurablack.handler.MediaHandler;
import de.shurablack.handler.StaticPageHandler;
import de.shurablack.http.Page;
import de.shurablack.http.PageStore;
import de.shurablack.websocket.LobbyWebSocket;
import it.sauronsoftware.cron4j.Scheduler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {

    private static final Logger LOGGER = LogManager.getLogger(Server.class);

    private static final Scheduler SCHEDULER = new Scheduler();

    private final ThreadPoolExecutor executor;

    private HttpsServer https;

    private final SSLContext sslContext;
    private LobbyWebSocket lobbyWebSocket;

    public static void startScheduler() {
        if (!SCHEDULER.isStarted()) {
            SCHEDULER.start();
        }
    }

    public Server() {
        this.sslContext = SSLBuilder.createSSLContext(LOGGER);

        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        this.executor.setKeepAliveTime(10, TimeUnit.SECONDS);
        this.executor.allowCoreThreadTimeOut(true);

        try {
            LOGGER.info("Prepare server for port...");

            this.https = HttpsServer.create(new InetSocketAddress(5001), 5);
            this.https.setExecutor(this.executor);

            LOGGER.info("Load pages into memory...");
            PageStore pageStore = new PageStore();
            pageStore.addPage("/", new Page("resources/index.html"));
            pageStore.addPage("/lobby", new Page("resources/lobby.html"));
            pageStore.addPage("/game", new Page("resources/game.html"));
            pageStore.addPage("/privacy", new Page("resources/privacy.html"));
            pageStore.addPage("/terms", new Page("resources/terms.html"));
            StaticPageHandler pageHandler = new StaticPageHandler(pageStore);

            LOGGER.info("Create default contexts for https server...");
            this.https.createContext("/", pageHandler);
            this.https.createContext("/create", new GameCreateHandler(this));
            this.https.createContext("/lobby", pageHandler);
            this.https.createContext("/game", pageHandler);
            this.https.createContext("/privacy", pageHandler);
            this.https.createContext("/terms", pageHandler);
            this.https.createContext("/media", new MediaHandler());
        } catch (IOException e) {
            LOGGER.error("Failed to create HTTPS server: {}", e.getMessage());
        }
    }

    public void start() {
        if (this.https == null) {
            LOGGER.error("Server not created");
            return;
        }

        if (this.sslContext == null) {
            LOGGER.error("SSL context not created");
            return;
        }

        LOGGER.info("Starting chatroom websocket on port {}...", 5002);
        this.lobbyWebSocket = new LobbyWebSocket(new InetSocketAddress(5002));
        this.lobbyWebSocket.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(this.sslContext));
        this.lobbyWebSocket.start();

        this.https.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                try {
                    // initialise the SSL context
                    SSLContext context = getSSLContext();
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // Set the SSL parameters
                    SSLParameters sslParameters = context.getSupportedSSLParameters();
                    params.setSSLParameters(sslParameters);

                } catch (Exception ex) {
                    LOGGER.error("Failed to create HTTPS parameters: {}", ex.getMessage());
                }
            }
        });

        LOGGER.info("Starting server...");
        this.https.start();
        LOGGER.info("Server is ready!");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Stopping server...");
            this.https.stop(0);
            try {
                if (SCHEDULER.isStarted()) {
                    SCHEDULER.stop();
                }

                if (!this.executor.isShutdown()) {
                    this.executor.shutdown();
                    if (this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        this.executor.shutdownNow();
                    }
                }

                this.lobbyWebSocket.stop(1,"Server closing");
            } catch (Exception e) {
                LOGGER.error("Failed to stop server: {}", e.getMessage());
                System.exit(1);
            }
        }));
    }

    public synchronized void createLobby(String id, boolean[] decks, String creatorUUID) {
        System.out.println("Creating lobby with ID: " + id + " and decks: " + decks.length + " and creator: " + creatorUUID);
        this.lobbyWebSocket.createLobby(id, decks);
    }

    public String getAvailableCode() {
        // 5 number/characters combination
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int random = (int) (Math.random() * 36);
            if (random < 10) {
                code.append(random);
            } else {
                code.append((char) ('A' + random - 10));
            }
        }
        return code.toString();
    }
}
