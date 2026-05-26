package com.aipet.agent;

import com.aipet.config.PetConfig;
import com.aipet.memory.PetMemory;
import com.aipet.memory.MemoryStore;
import com.aipet.sensor.SystemContextSensor;
import com.aipet.sensor.SystemSnapshot;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class PetAgent {
    private static final int PERSONA_CHANGE_REQUIRED_AFFINITY = 60;
    private final PetConfig config;
    private final MemoryStore memoryStore;
    private final SystemContextSensor sensor;
    private final DeepSeekClient client;
    private LocalDateTime lastProactiveAt = LocalDateTime.MIN;
    private String lastWindowTitle = "";
    private String apiState = "待机";

    public PetAgent(PetConfig config, MemoryStore memoryStore, SystemContextSensor sensor) {
        this.config = config;
        this.memoryStore = memoryStore;
        this.sensor = sensor;
        this.client = new DeepSeekClient(config);
    }

    public String replyToUser(String userInput) {
        SystemSnapshot snapshot = sensor.capture();
        String personaChangeReply = tryHandlePersonaChange(userInput);
        if (!personaChangeReply.isBlank()) {
            memoryStore.remember("主人请求修改人设：" + userInput + "；结果：" + personaChangeReply);
            return personaChangeReply;
        }
        apiState = "思考中";
        String reply = client.chat(List.of(
                new ChatMessage("system", systemPrompt()),
                new ChatMessage("user", "系统状态：\n" + snapshot.toPromptText() + "\n\n主人对你说：" + userInput)
        ));
        apiState = "待机";
        memoryStore.remember("主人说：" + userInput + "；你回复：" + reply);
        memoryStore.changeAffinity(2);
        updateMoodByText(reply);
        return reply;
    }

    public String maybeProactiveMessage() {
        SystemSnapshot snapshot = sensor.capture();
        if (!shouldSpeak(snapshot)) {
            return "";
        }
        lastProactiveAt = LocalDateTime.now();
        lastWindowTitle = snapshot.foregroundWindowTitle();
        apiState = "主动观察";
        String reply = client.chat(List.of(
                new ChatMessage("system", systemPrompt()),
                new ChatMessage("user", "请根据当前状态主动对主人说一句话，必须自然、简短，不超过45个中文字符。\n" + snapshot.toPromptText())
        ));
        apiState = "待机";
        memoryStore.remember("你观察到窗口「" + snapshot.foregroundWindowTitle() + "」并主动说：" + reply);
        updateMoodByText(reply);
        return reply;
    }

    public PetStatus currentStatus() {
        SystemSnapshot snapshot = sensor.capture();
        return new PetStatus(
                snapshot.time(),
                snapshot.foregroundWindowTitle(),
                memoryStore.getMemory().getMood(),
                memoryStore.getMemory().getAffinity(),
                inferActivity(snapshot.foregroundWindowTitle()),
                apiState
        );
    }

    private boolean shouldSpeak(SystemSnapshot snapshot) {
        long minutes = Duration.between(lastProactiveAt, LocalDateTime.now()).toMinutes();
        if (minutes < 3) {
            return false;
        }
        String title = snapshot.foregroundWindowTitle();
        if (title == null || title.isBlank()) {
            return minutes >= 8;
        }
        if (!title.equals(lastWindowTitle)) {
            return true;
        }
        int hour = snapshot.time().getHour();
        return minutes >= 10 || hour >= 23 || hour <= 5;
    }

    private String systemPrompt() {
        PetMemory memory = memoryStore.getMemory();
        String effectiveName = memory.getCustomName() == null || memory.getCustomName().isBlank()
                ? config.petName()
                : memory.getCustomName();
        String effectivePersonality = memory.getCustomPersonality() == null || memory.getCustomPersonality().isBlank()
                ? config.personality()
                : memory.getCustomPersonality();
        String speakingStyle = memory.getCustomSpeakingStyle() == null || memory.getCustomSpeakingStyle().isBlank()
                ? "保持可爱、简短、自然的中文表达。"
                : memory.getCustomSpeakingStyle();
        return effectivePersonality
                + "\n你的名字是：" + effectiveName
                + "\n你的说话方式：" + speakingStyle
                + "\n你是桌宠，不是网页聊天机器人。你能感知前台窗口、时间和长期记忆。"
                + "\n回复要求：中文，短句，像真实桌宠一样可爱，避免长篇解释。"
                + "\n长期状态：\n" + memory.toPromptText();
    }

    public String resetPersona() {
        memoryStore.resetPersona();
        String reply = "已经恢复初始设定啦，我又是小灵了～";
        memoryStore.remember("主人一键重置人设：" + reply);
        return reply;
    }

    private String tryHandlePersonaChange(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "";
        }
        String normalized = userInput.trim();
        boolean wantsPersonaChange = normalized.contains("改名")
                || normalized.contains("名字")
                || normalized.contains("叫你")
                || normalized.contains("你叫")
                || normalized.contains("人设")
                || normalized.contains("性格")
                || normalized.contains("说话方式")
                || normalized.contains("说话风格")
                || normalized.contains("语气");
        if (!wantsPersonaChange) {
            return "";
        }
        int affinity = memoryStore.getMemory().getAffinity();
        if (affinity < PERSONA_CHANGE_REQUIRED_AFFINITY) {
            return "现在好感度是 " + affinity + "，达到 " + PERSONA_CHANGE_REQUIRED_AFFINITY + " 才能改我的人设哦。多陪我聊聊吧～";
        }
        boolean changed = false;
        String name = extractAfterAny(normalized, "叫你", "你叫", "改名为", "名字改成", "名字叫");
        if (!name.isBlank()) {
            memoryStore.setCustomName(cleanValue(name));
            changed = true;
        }
        String personality = extractAfterAny(normalized, "人设改成", "人设是", "性格改成", "性格是");
        if (!personality.isBlank()) {
            memoryStore.setCustomPersonality(cleanValue(personality));
            changed = true;
        }
        String speakingStyle = extractAfterAny(normalized, "说话方式改成", "说话方式是", "说话风格改成", "说话风格是", "语气改成", "语气是");
        if (!speakingStyle.isBlank()) {
            memoryStore.setCustomSpeakingStyle(cleanValue(speakingStyle));
            changed = true;
        }
        if (!changed) {
            return "可以改，但你要说清楚一点，比如“你叫糖糖，语气改成傲娇一点”。";
        }
        return "好呀，我记住新设定了～" + personaSummary();
    }

    private String personaSummary() {
        PetMemory memory = memoryStore.getMemory();
        String name = memory.getCustomName() == null || memory.getCustomName().isBlank() ? config.petName() : memory.getCustomName();
        String personality = memory.getCustomPersonality() == null || memory.getCustomPersonality().isBlank() ? "默认人设" : memory.getCustomPersonality();
        String style = memory.getCustomSpeakingStyle() == null || memory.getCustomSpeakingStyle().isBlank() ? "默认语气" : memory.getCustomSpeakingStyle();
        return "现在我叫「" + name + "」，人设：「" + personality + "」，说话方式：「" + style + "」。";
    }

    private String extractAfterAny(String text, String... markers) {
        for (String marker : markers) {
            int index = text.indexOf(marker);
            if (index >= 0) {
                return firstSegment(text.substring(index + marker.length()));
            }
        }
        return "";
    }

    private String firstSegment(String text) {
        String[] separators = {"，", ",", "。", "！", "!", "；", ";", "然后", "并且", "而且"};
        int end = text.length();
        for (String separator : separators) {
            int index = text.indexOf(separator);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return text.substring(0, end);
    }

    private String cleanValue(String value) {
        String cleaned = value.replace("。", "")
                .replace("，", " ")
                .replace(",", " ")
                .replace("！", "")
                .replace("!", "")
                .trim();
        int maxLength = 80;
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private String inferActivity(String title) {
        if (title == null || title.isBlank()) {
            return "发呆";
        }
        String lower = title.toLowerCase();
        if (lower.contains("idea") || lower.contains("code") || lower.contains(".java") || lower.contains(".py")) {
            return "写代码";
        }
        if (lower.contains("chrome") || lower.contains("edge") || lower.contains("browser")) {
            return "上网";
        }
        if (lower.contains("steam") || lower.contains("game") || lower.contains("valorant")) {
            return "玩游戏";
        }
        if (lower.contains("word") || lower.contains("wps") || lower.contains(".doc")) {
            return "写文档";
        }
        return "观察中";
    }

    private void updateMoodByText(String text) {
        if (text == null) {
            return;
        }
        if (text.contains("休息") || text.contains("累")) {
            memoryStore.setMood("担心");
        } else if (text.contains("哈哈") || text.contains("开心") || text.contains("棒")) {
            memoryStore.setMood("开心");
        } else if (text.contains("游戏") || text.contains("开冲")) {
            memoryStore.setMood("兴奋");
        } else {
            memoryStore.setMood("好奇");
        }
    }
}
