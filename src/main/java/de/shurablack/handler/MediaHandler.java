package de.shurablack.handler;

import de.shurablack.http.ContentType;
import de.shurablack.http.Request;
import de.shurablack.http.RequestHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MediaHandler extends RequestHandler {

    private final Map<String, MediaEntry> mediaMap = Map.of(
            "/media/logo.png", new MediaEntry("resources/media/logo.png", ContentType.PNG),
            "/media/favicon.png", new MediaEntry("resources/media/favicon.png", ContentType.PNG),
            "/media/background-music.mp3", new MediaEntry("resources/media/background-music.mp3", ContentType.MP3)
    );

    @Override
    public void get(Request request) {
        MediaEntry mediaEntry = mediaMap.get(request.getPath());

        if (mediaEntry == null) {
            request.sendEmptyResponse(404);
            return;
        }

        if (mediaEntry.getType().equals(ContentType.MP3)) {
            request.addResponseHeader("Connection", "keep-alive");
            request.addResponseHeader("Accept-Ranges", "bytes 0-" + (mediaEntry.getData().length - 1));
        }

        request.sendResponseWithClose(200, mediaEntry.getData(), mediaEntry.getType());
    }

    @Override
    public void options(Request request) {
        request.addResponseHeader("Access-Control-Allow-Origin", "*");
        request.addResponseHeader("Access-Control-Allow-Methods", "OPTIONS");
        request.addResponseHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        request.sendEmptyResponse(200);
    }

    private static class MediaEntry {

        private byte[] data;
        private ContentType type;

        public MediaEntry(String path, ContentType type) {
            try {
                this.data = Files.readAllBytes(Path.of(path));
                this.type = type;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public byte[] getData() {
            return data;
        }

        public ContentType getType() {
            return type;
        }
    }
}
