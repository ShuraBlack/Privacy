package de.shurablack.http;

import java.util.HashMap;
import java.util.Map;

public class PageStore {

    public static final ContentType DEFAULT_CONTENT_TYPE = ContentType.HTML;
    public final Map<String, Page> pages = new HashMap<>();

    public void addPage(String path, Page page) {
        pages.put(path, page);
    }

    public Page getPage(String path) {
        return pages.get(path);
    }
}
