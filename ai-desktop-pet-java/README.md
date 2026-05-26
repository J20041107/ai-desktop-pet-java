# AI Desktop Pet Java

一个用 JavaFX 编写的 AI 桌宠项目，支持透明悬浮窗口、可拖动、点击聊天、主动观察当前窗口并通过 DeepSeek API 生成回复。

## 技术栈

- Java 17
- Maven
- JavaFX
- DeepSeek OpenAI-compatible Chat API
- JNA Windows 前台窗口感知
- JSON 本地长期记忆

## 配置 DeepSeek

推荐使用环境变量：

```powershell
setx DEEPSEEK_API_KEY "你的 DeepSeek API Key"
```

也可以编辑：

```text
src/main/resources/config.properties
```

填写：

```properties
deepseek.api.key=你的 DeepSeek API Key
```

如果你实际使用的模型名不是 `deepseek-chat`，请修改：

```properties
deepseek.model=你的模型名
```

## 运行

```powershell
.\run.bat
```

当前项目的 `run.bat` 和 `build.bat` 已配置为使用 IntelliJ IDEA 自带的 Java 17 与 Maven，因此不要求系统全局安装 Maven。

## 当前功能

- 透明桌面悬浮窗
- 鼠标拖动桌宠
- 点击桌宠聚焦输入框
- 主动观察前台窗口并发言
- 实时显示活动、情绪、好感度、AI 状态和当前窗口
- 点击 `换图片` 从本地上传并切换桌宠图片
- 好感度达到 60 后，可通过对话修改桌宠名字、人设和说话方式
- 点击 `重置人设` 一键恢复默认小灵设定
- 保存好感度、情绪和近期记忆到 `pet-memory.json`

## 自定义桌宠图片

运行桌宠后，点击界面里的 `换图片`，选择本地图片即可。

程序会自动把图片复制到 `assets/` 目录，并写入：

```text
src/main/resources/config.properties
```

支持 `png`、`jpg`、`jpeg`、`gif`、`webp` 等常见图片格式。下次启动会继续使用你上次选择的图片。

## 修改人设

当好感度达到 `60` 后，可以直接对桌宠说：

```text
你叫糖糖
```

```text
语气改成傲娇一点
```

```text
人设改成元气满满的游戏搭子
```

也可以点击 `重置人设`，恢复最初的 `小灵` 设定。

## 后续可扩展

- Live2D 或 GIF 动画角色
- TTS 语音播放
- 开机自启
- 更复杂的情绪状态机
- 与音乐播放器、日历、游戏状态联动
