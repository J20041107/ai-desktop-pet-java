package com.aipet.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public record PetConfig(
        String deepSeekApiKey,
        String deepSeekApiUrl,
        String deepSeekModel,
        String petName,
        String personality,
        Path petImagePath,
        Path memoryFile
) {
    public static PetConfig load() {
        Properties properties = new Properties();
        try (InputStream input = PetConfig.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }

        Map<String, String> envFile = loadEnvFile(Path.of(".env"));
        String apiKey = firstNonBlank(envFile.get("DEEPSEEK_API_KEY"),
                firstNonBlank(System.getenv("DEEPSEEK_API_KEY"), properties.getProperty("deepseek.api.key", "")));
        String apiUrl = properties.getProperty("deepseek.api.url", "https://api.deepseek.com/chat/completions");
        String model = properties.getProperty("deepseek.model", "deepseek-chat");
        String petName = properties.getProperty("pet.name", "小灵");
        String personality = properties.getProperty("pet.personality", "你是一个可爱的中文桌面宠物。");
        Path petImagePath = Path.of(properties.getProperty("pet.image.path", "assets/pet.png"));
        Path memoryFile = Path.of(properties.getProperty("memory.file", "pet-memory.json"));

        return new PetConfig(apiKey, apiUrl, model, petName, personality, petImagePath, memoryFile);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private static Map<String, String> loadEnvFile(Path path) {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int index = trimmed.indexOf('=');
                String key = trimmed.substring(0, index).trim();
                String value = trimmed.substring(index + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load .env", e);
        }
        return values;
    }
}
