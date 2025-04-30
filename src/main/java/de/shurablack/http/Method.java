package de.shurablack.http;

public enum Method {
    GET, POST, PUT, DELETE, HEAD, OPTIONS, UNSUPPORTED;

    public static Method fromString(String method) {
        switch (method) {
            case "GET":
                return GET;
            case "POST":
                return POST;
            case "PUT":
                return PUT;
            case "DELETE":
                return DELETE;
            case "HEAD":
                return HEAD;
            case "OPTIONS":
                return OPTIONS;
            default:
                return UNSUPPORTED;
        }
    }
}
