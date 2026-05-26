package com.aipet.sensor;

import java.time.LocalDateTime;

public record SystemSnapshot(
        LocalDateTime time,
        String foregroundWindowTitle,
        double systemLoad,
        long freeMemoryMb,
        long totalMemoryMb
) {
    public String toPromptText() {
        return "当前时间：" + time
                + "\n前台窗口：" + foregroundWindowTitle
                + "\n系统负载：" + String.format("%.2f", systemLoad)
                + "\nJVM内存：" + freeMemoryMb + "MB / " + totalMemoryMb + "MB 可用";
    }
}
