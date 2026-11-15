package com.attendx.ui.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    private static final String DEFAULT_BASE = "http://localhost:8080/api/v1";
    private final String baseUrl;
    private final HttpClient http;
    static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ApiClient() {
        String prop = System.getProperty("attendx.api");
        if (prop == null || prop.isBlank() || prop.contains("${")) {
            this.baseUrl = DEFAULT_BASE;
        } else {
            prop = prop.trim();
            this.baseUrl = prop.endsWith("/") ? prop.substring(0, prop.length() - 1) : prop;
        }
        System.out.println("[AttendX] Initialized ApiClient with baseUrl: " + this.baseUrl);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public ApiClient(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank() || baseUrl.contains("${")) {
            this.baseUrl = DEFAULT_BASE;
        } else {
            baseUrl = baseUrl.trim();
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }
        System.out.println("[AttendX] Initialized ApiClient with custom baseUrl: " + this.baseUrl);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private URI resolveUri(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return URI.create(path);
        }
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(baseUrl + cleanPath);
    }

    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(resolveUri(path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> post(String path, Object body) throws IOException, InterruptedException {
        String json = MAPPER.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(resolveUri(path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(resolveUri(path))
                .timeout(Duration.ofSeconds(10))
                .DELETE()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public <T> T parse(String json, Class<T> cls) throws IOException {
        return MAPPER.readValue(json, cls);
    }

    public <T> T parse(String json, com.fasterxml.jackson.core.type.TypeReference<T> ref) throws IOException {
        return MAPPER.readValue(json, ref);
    }

    public String getBaseUrl() { return baseUrl; }
}
