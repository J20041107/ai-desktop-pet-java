package com.aipet.memory;

import java.util.ArrayList;
import java.util.List;

public class PetMemory {
    private int affinity = 50;
    private String mood = "好奇";
    private String customName = "";
    private String customPersonality = "";
    private String customSpeakingStyle = "";
    private final List<String> recentEvents = new ArrayList<>();

    public int getAffinity() {
        return affinity;
    }

    public void setAffinity(int affinity) {
        this.affinity = Math.max(0, Math.min(100, affinity));
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName == null ? "" : customName;
    }

    public String getCustomPersonality() {
        return customPersonality;
    }

    public void setCustomPersonality(String customPersonality) {
        this.customPersonality = customPersonality == null ? "" : customPersonality;
    }

    public String getCustomSpeakingStyle() {
        return customSpeakingStyle;
    }

    public void setCustomSpeakingStyle(String customSpeakingStyle) {
        this.customSpeakingStyle = customSpeakingStyle == null ? "" : customSpeakingStyle;
    }

    public void resetPersona() {
        customName = "";
        customPersonality = "";
        customSpeakingStyle = "";
    }

    public List<String> getRecentEvents() {
        return recentEvents;
    }

    public void addEvent(String event) {
        if (event == null || event.isBlank()) {
            return;
        }
        recentEvents.add(event);
        while (recentEvents.size() > 20) {
            recentEvents.remove(0);
        }
    }

    public String toPromptText() {
        return "好感度：" + affinity
                + "\n当前情绪：" + mood
                + "\n自定义名字：" + displayOrDefault(customName, "未设置")
                + "\n自定义人设：" + displayOrDefault(customPersonality, "未设置")
                + "\n自定义说话方式：" + displayOrDefault(customSpeakingStyle, "未设置")
                + "\n近期记忆：" + String.join("；", recentEvents);
    }

    private String displayOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
