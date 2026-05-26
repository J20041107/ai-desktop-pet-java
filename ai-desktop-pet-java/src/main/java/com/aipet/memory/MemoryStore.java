package com.aipet.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MemoryStore {
    private final Path path;
    private final ObjectMapper mapper;
    private PetMemory memory;

    public MemoryStore(Path path) {
        this.path = path;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.memory = load();
    }

    public synchronized PetMemory getMemory() {
        return memory;
    }

    public synchronized void remember(String event) {
        memory.addEvent(event);
        save();
    }

    public synchronized void changeAffinity(int delta) {
        memory.setAffinity(memory.getAffinity() + delta);
        save();
    }

    public synchronized void setMood(String mood) {
        memory.setMood(mood);
        save();
    }

    public synchronized void setCustomName(String name) {
        memory.setCustomName(name);
        save();
    }

    public synchronized void setCustomPersonality(String personality) {
        memory.setCustomPersonality(personality);
        save();
    }

    public synchronized void setCustomSpeakingStyle(String speakingStyle) {
        memory.setCustomSpeakingStyle(speakingStyle);
        save();
    }

    public synchronized void resetPersona() {
        memory.resetPersona();
        save();
    }

    private PetMemory load() {
        if (!Files.exists(path)) {
            return new PetMemory();
        }
        try {
            return mapper.readValue(path.toFile(), PetMemory.class);
        } catch (IOException e) {
            return new PetMemory();
        }
    }

    private void save() {
        try {
            mapper.writeValue(path.toFile(), memory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save pet memory", e);
        }
    }
}
