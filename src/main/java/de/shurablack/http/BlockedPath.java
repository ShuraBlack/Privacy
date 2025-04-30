package de.shurablack.http;

import java.util.function.BiFunction;

public class BlockedPath {
    private final Method method;
    private final String path;
    private final BiFunction<String, String, Boolean> comparison;

    private BlockedPath(Method method, String path, BiFunction<String, String, Boolean> comparison) {
        this.method = method;
        this.path = path;
        this.comparison = comparison;
    }

    public static BlockedPath of(Method method, String path, BiFunction<String, String, Boolean> comparison) {
        return new BlockedPath(method, path, comparison);
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public BiFunction<String, String, Boolean> getComparison() {
        return comparison;
    }
}
