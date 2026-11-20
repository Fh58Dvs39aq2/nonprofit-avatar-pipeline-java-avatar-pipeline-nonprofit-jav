package org.example.avatar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class AvatarService {
    private final InfraiClient client;

    public AvatarService(InfraiClient client) { this.client = client; }

    public String process(Path file, String filename, String userId) throws IOException, InterruptedException {
        String bytes = java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(file));
        String uploaded = client.post("/v1/image/upload", "{\"file\":\"" + jsonString(bytes) + "\",\"filename\":\"" + jsonString(filename) + "\"}");
        String image = bytes;
        int side = squareSide(file);
        String cropped = client.post("/v1/image/smart_crop", "{\"image\":{\"base64\":\"" + jsonString(image) + "\"},\"aspect\":\"1:1\"}");
        String croppedImage = value(cropped, "url");
        String compressed = client.post("/v1/image/resize", "{\"image\":{\"base64\":\"" + jsonString(croppedImage.substring(croppedImage.indexOf(',') + 1)) + "\"},\"width\":" + side + ",\"height\":" + side + ",\"fit\":\"inside\",\"enlarge\":false,\"format\":\"jpeg\",\"store\":true}");
        String finalImage = value(compressed, "url");
        return client.patch("/v1/auth/user/update/" + pathSegment(userId), "{\"user_id\":\"" + jsonString(userId) + "\",\"metadata\":{\"avatar\":\"" + jsonString(finalImage) + "\"}}");
    }

    public static int squareSide(Path file) throws IOException {
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file.toFile());
        if (image == null) throw new IOException("unsupported image format");
        return Math.min(2048, Math.min(image.getWidth(), image.getHeight()));
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) escaped.append(String.format("\\u%04x", (int) c));
                    else escaped.append(c);
                }
            }
        }
        return escaped.toString();
    }

    private static String pathSegment(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String value(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) throw new IllegalStateException("missing response value");
        start += marker.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length() || json.charAt(start) != '"') throw new IllegalStateException("missing response value");
        start++;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { value.append(c); escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') return value.toString();
            value.append(c);
        }
        throw new IllegalStateException("missing response value");
    }
}
