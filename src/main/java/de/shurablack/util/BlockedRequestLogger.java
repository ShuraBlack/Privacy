package de.shurablack.util;

import de.shurablack.http.Method;
import de.shurablack.http.Request;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class BlockedRequestLogger {

    public static void log(Request request) {
        try (FileWriter fw = new FileWriter("blocked_requests.log", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            String inputStream = "";
            if (request.getMethod().equals(Method.POST)) {
                inputStream = request.bodyAsString().orElse("");
                inputStream = inputStream.isEmpty() ? "" : "\nwith body: " + inputStream;
            }

            out.println(String.format("%s - %s|%s from %s%s",
                    formattedTimestamp(),
                    request.getMethod().toString(),
                    request.getPath(),
                    request.getOrigin().getRemoteAddress().getAddress().toString(),
                    inputStream
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formattedTimestamp() {
        final LocalDateTime now = LocalDateTime.now();
        return String.format("%d-%02d-%02d %02d:%02d:%02d",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                now.getMinute(),
                now.getSecond());
    }
}
