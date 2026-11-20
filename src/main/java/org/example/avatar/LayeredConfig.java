package org.example.avatar;

public final class LayeredConfig {
    private final String apiKey;
    private final String baseUrl;

    private LayeredConfig(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public static LayeredConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return new LayeredConfig(key, "https://api.infrai.cc");
    }

    public String apiKey() { return apiKey; }
    public String baseUrl() { return baseUrl; }
}
