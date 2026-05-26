package com.aipet.ui;

import com.aipet.agent.PetAgent;
import com.aipet.agent.PetStatus;
import com.aipet.config.PetConfig;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class PetWindow {
    private final Stage stage;
    private final PetAgent agent;
    private final PetConfig config;
    private final Label bubble = new Label("点我聊天呀～");
    private final Label status = new Label("状态加载中...");
    private final TextField input = new TextField();
    private final VBox historyList = new VBox(6);
    private final ScrollPane historyPane = new ScrollPane(historyList);
    private VBox controlPanel;
    private Button closeButton;
    private boolean historyVisible = false;
    private boolean controlsVisible = false;
    private boolean locked = false;
    private Path currentPetImagePath;
    private double dragOffsetX;
    private double dragOffsetY;

    public PetWindow(Stage stage, PetAgent agent, PetConfig config) {
        this.stage = stage;
        this.agent = agent;
        this.config = config;
        this.currentPetImagePath = config.petImagePath();
    }

    public void show() {
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setTitle("AI 桌宠");
        status.getStyleClass().add("status-label");
        historyPane.getStyleClass().add("history-pane");
        historyList.getStyleClass().add("history-list");
        historyPane.setFitToWidth(true);
        historyPane.setPrefHeight(110);
        historyPane.setVisible(false);
        historyPane.setManaged(false);

        closeButton = createCloseButton();
        closeButton.setVisible(false);
        closeButton.setManaged(false);
        StackPane petBody = createPetBody();
        controlPanel = createControlPanel();
        VBox root = new VBox(8, bubble, petBody, status, controlPanel);
        root.setPadding(new Insets(10));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: transparent;");

        StackPane windowRoot = new StackPane(root, closeButton);
        windowRoot.getStyleClass().add("transparent-root");
        windowRoot.setStyle("-fx-background-color: transparent;");
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(6, 6, 0, 0));

        Scene scene = new Scene(windowRoot, 360, 430);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setX(1200);
        stage.setY(580);
        stage.show();

        windowRoot.setOnContextMenuRequested(event -> toggleControls());
        installDrag(windowRoot);
        installStatusLoop();
        installProactiveLoop();
    }

    private Button createCloseButton() {
        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("window-close-button");
        closeButton.setOnMousePressed(event -> event.consume());
        closeButton.setOnAction(event -> confirmExit());
        return closeButton;
    }

    private StackPane createPetBody() {
        ImageView customImage = createCustomImage();
        if (customImage != null) {
            StackPane pet = new StackPane(customImage);
            installPetClick(pet);
            return pet;
        }
        Circle body = new Circle(58, Color.web("#ffd6e7"));
        Circle face = new Circle(42, Color.web("#fff7fb"));
        Label emoji = new Label("ฅ^•ﻌ•^ฅ");
        emoji.getStyleClass().add("pet-face");
        StackPane pet = new StackPane(body, face, emoji);
        installPetClick(pet);
        return pet;
    }

    private ImageView createCustomImage() {
        if (currentPetImagePath == null || !Files.exists(currentPetImagePath)) {
            return null;
        }
        Image image = new Image(currentPetImagePath.toUri().toString(), 120, 120, true, true, true);
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("pet-image");
        return imageView;
    }

    private void installPetClick(StackPane pet) {
        pet.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                say(controlsVisible ? "我在听哦～" : "右键我，就能打开菜单啦。");
            }
        });
    }

    private VBox createControlPanel() {
        input.setPromptText("和桌宠说句话...");
        input.setMaxWidth(210);
        Button send = new Button("发送");
        Button changeImage = new Button("更换桌宠样式");
        Button toggleHistory = new Button("对话历史");
        Button resetPersona = new Button("重置到初始状态");
        Button toggleLock = new Button("锁定桌宠");
        send.setOnAction(event -> sendUserMessage());
        changeImage.setOnAction(event -> choosePetImage());
        toggleHistory.setOnAction(event -> {
            historyVisible = !historyVisible;
            historyPane.setVisible(historyVisible);
            historyPane.setManaged(historyVisible);
            toggleHistory.setText(historyVisible ? "隐藏对话历史" : "对话历史");
        });
        resetPersona.setOnAction(event -> resetPersona());
        toggleLock.setOnAction(event -> {
            locked = !locked;
            toggleLock.setText(locked ? "解锁桌宠" : "锁定桌宠");
            say(locked ? "位置锁定啦，右键还能打开菜单～" : "已经解锁，可以拖动我啦。");
        });
        input.setOnAction(event -> sendUserMessage());
        VBox buttons = new VBox(6, send, changeImage, toggleHistory, resetPersona, toggleLock);
        buttons.getStyleClass().add("side-button-bar");
        buttons.setAlignment(Pos.CENTER);
        VBox editor = new VBox(8, input, historyPane);
        editor.setAlignment(Pos.CENTER);
        HBox content = new HBox(10, editor, buttons);
        content.setAlignment(Pos.CENTER);
        VBox box = new VBox(content);
        box.getStyleClass().add("control-panel");
        box.setAlignment(Pos.CENTER);
        box.setVisible(false);
        box.setManaged(false);
        return box;
    }

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        controlPanel.setVisible(controlsVisible);
        controlPanel.setManaged(controlsVisible);
        closeButton.setVisible(controlsVisible);
        closeButton.setManaged(controlsVisible);
    }

    private void confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("退出确认");
        alert.setHeaderText("真的要退出桌宠吗？");
        alert.setContentText("小灵会在这里等你下次回来。");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
        }
    }

    private void resetPersona() {
        String reply = agent.resetPersona();
        say(reply);
        addHistory("系统", reply);
    }

    private void choosePetImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择桌宠图片");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) {
            return;
        }
        try {
            Files.createDirectories(Path.of("assets"));
            String extension = "";
            int dotIndex = selected.getName().lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = selected.getName().substring(dotIndex);
            }
            Path target = Path.of("assets", "pet-custom" + extension);
            Files.copy(selected.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            currentPetImagePath = target;
            savePetImagePath(target);
            say("图片已保存，重启后会显示新造型～");
        } catch (IOException e) {
            say("换图片失败：" + e.getMessage());
        }
    }

    private void savePetImagePath(Path imagePath) throws IOException {
        Path configPath = Path.of("src", "main", "resources", "config.properties");
        String key = "pet.image.path=";
        String value = key + imagePath.toString().replace("\\", "/");
        String content = Files.exists(configPath) ? Files.readString(configPath) : "";
        if (content.contains(key)) {
            content = content.replaceAll("(?m)^pet\\.image\\.path=.*$", value);
        } else {
            content = content + System.lineSeparator() + value + System.lineSeparator();
        }
        Files.writeString(configPath, content);
    }

    private void sendUserMessage() {
        String text = input.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        input.clear();
        addHistory("你", text);
        say("思考中...");
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return agent.replyToUser(text);
            }
        };
        task.setOnSucceeded(event -> {
            say(task.getValue());
            addHistory("桌宠", task.getValue());
        });
        task.setOnFailed(event -> {
            String message = "我卡住了，等会再试试嘛。";
            say(message);
            addHistory("系统", message);
        });
        Thread thread = new Thread(task, "pet-user-chat");
        thread.setDaemon(true);
        thread.start();
    }

    private void installDrag(Pane root) {
        root.setOnMousePressed(event -> {
            if (locked || event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            dragOffsetX = event.getScreenX() - stage.getX();
            dragOffsetY = event.getScreenY() - stage.getY();
        });
        root.setOnMouseDragged(event -> {
            if (locked || event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            stage.setX(event.getScreenX() - dragOffsetX);
            stage.setY(event.getScreenY() - dragOffsetY);
        });
    }

    private void installStatusLoop() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            PetStatus petStatus = agent.currentStatus();
            String windowTitle = petStatus.foregroundWindowTitle();
            if (windowTitle != null && windowTitle.length() > 18) {
                windowTitle = windowTitle.substring(0, 18) + "...";
            }
            status.setText(petStatus.activity()
                    + " | " + petStatus.mood()
                    + " | 好感 " + petStatus.affinity()
                    + "\n" + petStatus.apiState()
                    + " | " + windowTitle);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void installProactiveLoop() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(20), event -> {
            Task<String> task = new Task<>() {
                @Override
                protected String call() {
                    return agent.maybeProactiveMessage();
                }
            };
            task.setOnSucceeded(done -> {
                String message = task.getValue();
                if (message != null && !message.isBlank()) {
                    say(message);
                    addHistory("桌宠", message);
                }
            });
            Thread thread = new Thread(task, "pet-proactive-loop");
            thread.setDaemon(true);
            thread.start();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void say(String text) {
        bubble.setText(text == null ? "……" : text);
    }

    private void addHistory(String speaker, String text) {
        Label item = new Label(speaker + "：" + (text == null ? "" : text));
        item.getStyleClass().add("history-item");
        historyList.getChildren().add(item);
        Platform.runLater(() -> historyPane.setVvalue(1.0));
    }
}
