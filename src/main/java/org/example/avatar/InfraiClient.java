package org.example.avatar;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

public final class InfraiClient {
    private final LayeredConfig config;
    private final HttpClient http = HttpClient.newHttpClient();

    public InfraiClient(LayeredConfig config) { this.config = config; }

    // The canonical capability idiom is infrai.image.upload; the Java client keeps it as a REST boundary.
    public String post(String path, String body) throws IOException, InterruptedException {
        return send("POST", path, body);
    }

    public String patch(String path, String body) throws IOException, InterruptedException {
        return send("PATCH", path, body);
    }

    private String send(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (response.statusCode() == 429 && attempt < 3) {
                long retry = response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L << attempt);
                Thread.sleep(retry * 1000L);
                continue;
            }
            Object decoded = new Json(envelope).parse();
            if (!(decoded instanceof Map<?, ?> fields) || !(fields.get("ok") instanceof Boolean))
                throw new IOException("invalid Infrai response envelope");
            if (Boolean.FALSE.equals(fields.get("ok")))
                throw new IllegalStateException("Infrai request rejected (HTTP " + response.statusCode()
                        + "): " + fields.get("error"));
            if (response.statusCode() >= 400)
                throw new IOException("remote service response " + response.statusCode());
            return envelope;
        }
        throw new IOException("request retries exhausted");
    }

    // Small JSON decoder keeps the documented JDK-only entry point dependency-free.
    private static final class Json {
        private final String text;
        private int at;
        Json(String text) { this.text = text; }
        Object parse() throws IOException {
            Object result = value(0);
            space();
            if (at != text.length()) throw invalid();
            return result;
        }
        private Object value(int depth) throws IOException {
            if (depth > 128) throw invalid();
            space();
            if (at == text.length()) throw invalid();
            char c = text.charAt(at);
            if (c == '"') return string();
            if (c == '{') {
                at++;
                Map<String, Object> result = new LinkedHashMap<>();
                if (take('}')) return result;
                do {
                    space();
                    if (at == text.length() || text.charAt(at) != '"') throw invalid();
                    String key = string();
                    if (!take(':') || result.containsKey(key)) throw invalid();
                    result.put(key, value(depth + 1));
                } while (take(','));
                if (!take('}')) throw invalid();
                return result;
            }
            if (c == '[') {
                at++;
                var result = new ArrayList<Object>();
                if (take(']')) return result;
                do { result.add(value(depth + 1)); } while (take(','));
                if (!take(']')) throw invalid();
                return result;
            }
            for (String literal : new String[]{"true", "false", "null"}) {
                if (text.startsWith(literal, at)) {
                    at += literal.length();
                    return literal.equals("null") ? null : Boolean.valueOf(literal);
                }
            }
            var number = java.util.regex.Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")
                    .matcher(text).region(at, text.length());
            if (!number.lookingAt()) throw invalid();
            at = number.end();
            return number.group();
        }
        private String string() throws IOException {
            at++;
            StringBuilder result = new StringBuilder();
            while (at < text.length()) {
                char c = text.charAt(at++);
                if (c == '"') return result.toString();
                if (c < 0x20) throw invalid();
                if (c == '\\') {
                    if (at == text.length()) throw invalid();
                    c = text.charAt(at++);
                    switch (c) {
                        case '"', '\\', '/' -> result.append(c);
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case 'n' -> result.append('\n');
                        case 'r' -> result.append('\r');
                        case 't' -> result.append('\t');
                        case 'u' -> {
                            if (at + 4 > text.length()) throw invalid();
                            int code = 0;
                            for (int i = 0; i < 4; i++) {
                                int digit = Character.digit(text.charAt(at++), 16);
                                if (digit < 0) throw invalid();
                                code = code * 16 + digit;
                            }
                            result.append((char) code);
                        }
                        default -> throw invalid();
                    }
                } else result.append(c);
            }
            throw invalid();
        }
        private void space() {
            while (at < text.length() && " \t\r\n".indexOf(text.charAt(at)) >= 0) at++;
        }
        private boolean take(char c) {
            space();
            if (at < text.length() && text.charAt(at) == c) { at++; return true; }
            return false;
        }
        private IOException invalid() { return new IOException("invalid Infrai JSON response at " + at); }
    }
}
