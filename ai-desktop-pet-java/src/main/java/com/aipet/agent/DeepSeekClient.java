package com.aipet.agent;

import com.aipet.config.PetConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class DeepSeekClient {
    private final PetConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeepSeekClient(PetConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public String chat(List<ChatMessage> messages) {
        if (config.deepSeekApiKey() == null || config.deepSeekApiKey().isBlank()) {
            return "还没有配置 DeepSeek API Key，先在 config.properties 或环境变量 DEEPSEEK_API_KEY 里填一下哦。";
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", config.deepSeekModel(),
                    "messages", messages,
                    "temperature", 0.85,
                    "max_tokens", 220
            );
            String json = mapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.deepSeekApiUrl()))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + config.deepSeekApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "DeepSeek 调用失败：HTTP " + response.statusCode();
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() ? "我刚刚有点走神，没组织好语言。" : content.asText();
        } catch (IOException e) {
            return "网络或响应解析失败：" + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "请求被打断啦。";
        }
    }
}
