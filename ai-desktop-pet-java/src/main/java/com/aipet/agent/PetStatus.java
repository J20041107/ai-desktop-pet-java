package com.aipet.agent;

import java.time.LocalDateTime;

public record PetStatus(
        LocalDateTime time,
        String foregroundWindowTitle,
        String mood,
        int affinity,
        String activity,
        String apiState
) {
}
