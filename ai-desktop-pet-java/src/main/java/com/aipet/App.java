package com.aipet;

import com.aipet.agent.PetAgent;
import com.aipet.config.PetConfig;
import com.aipet.memory.MemoryStore;
import com.aipet.sensor.SystemContextSensor;
import com.aipet.ui.PetWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        PetConfig config = PetConfig.load();
        MemoryStore memoryStore = new MemoryStore(config.memoryFile());
        SystemContextSensor sensor = new SystemContextSensor();
        PetAgent agent = new PetAgent(config, memoryStore, sensor);
        PetWindow window = new PetWindow(stage, agent, config);
        window.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
