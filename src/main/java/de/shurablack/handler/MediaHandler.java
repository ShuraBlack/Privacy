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

        // if it contains a range header, we need to send a partial response
        String rangeHeader = request.getOrigin().getRequestHeaders().getFirst("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : mediaEntry.getData().length - 1;

            if (start >= mediaEntry.getData().length || end >= mediaEntry.getData().length) {
                request.sendEmptyResponse(416);
                return;
            }

            byte[] partialData = new byte[(int) (end - start + 1)];
            System.arraycopy(mediaEntry.getData(), (int) start, partialData, 0, partialData.length);

            request.addResponseHeader("Accept-Ranges", "bytes");
            request.addResponseHeader("Content-Length", String.valueOf(partialData.length));
            request.addResponseHeader("Content-Range", "bytes " + start + "-" + end + "/" + mediaEntry.getData().length);
            request.sendResponseWithClose(206, partialData, mediaEntry.getType());
            return;
        }

        request.addResponseHeader("Accept-Ranges", "bytes");
        request.addResponseHeader("Content-Length", String.valueOf(mediaEntry.getData().length));
        request.addResponseHeader("Content-Range", "bytes 0-" + (mediaEntry.getData().length - 1) + "/" + mediaEntry.getData().length);
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
