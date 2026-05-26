package com.aipet.sensor;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;

public class SystemContextSensor {
    private final WindowSensor windowSensor = new WindowSensor();

    public SystemSnapshot capture() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        Runtime runtime = Runtime.getRuntime();
        long freeMemoryMb = runtime.freeMemory() / 1024 / 1024;
        long totalMemoryMb = runtime.totalMemory() / 1024 / 1024;
        double systemLoad = osBean == null ? -1 : osBean.getCpuLoad();
        return new SystemSnapshot(
                LocalDateTime.now(),
                windowSensor.getForegroundWindowTitle(),
                systemLoad,
                freeMemoryMb,
                totalMemoryMb
        );
    }
}
