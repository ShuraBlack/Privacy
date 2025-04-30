package de.shurablack.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Page {

    private static final Logger LOGGER = LogManager.getLogger(Page.class);

    private byte[] pageBytes;

    public Page(final String path) {
        try {
            this.pageBytes = Files.readAllBytes(Path.of(path));
            if (this.pageBytes.length == 0) {
                LOGGER.error("Empty page loaded: {}", path);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read file: {}", e.getMessage());
        }
    }

    public byte[] getBytes() {
        return pageBytes;
    }
}
