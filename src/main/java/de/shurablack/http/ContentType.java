package de.shurablack.http;

public enum ContentType {
    JSON("application/json"),
    HTML("text/html"),
    PNG("image/png"),
    JPG("image/jpeg"),
    ICO("image/x-icon"),
    TXT("text/plain"),
    MP3("audio/mpeg");

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
