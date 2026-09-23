# 开发指南

Codex 的仓库开发约定统一维护在根目录 [AGENTS.md](../AGENTS.md)。

## 工程入口

本项目是单模块 Android Gradle 工程，应用模块为 `app`，不涉及 AOSP System/Vendor 分仓。

- 构建配置：根目录和 `app/build.gradle.kts`。
- 配置与历史数据：`core/Prefs.kt`、`core/kb/`。
- 采集分发：`capture/ChatCaptureService.kt`。
- 模型调用：`jev/JudgeClient.kt`、`ReplyClient.kt`、`VisionClient.kt`。
- UI：`MainActivity.kt`、`SettingsActivity.kt`、`KnowledgeActivity.kt` 与 `overlay/`。

产品名为“对话攻略”，英文副标为“Dialogue Route”，包名为 `io.github.liaong13.dialogueroute`。包名与上游不同，因此会作为独立应用安装；旧包的配置和私有数据不会自动迁移。

## Release 签名

未设置 `DIALOGUE_ROUTE_KEYSTORE_PROPS` 时，`./gradlew :app:assembleRelease` 生成未签名 APK。

需要签名时，在仓库外创建 properties 文件，填入：

```properties
storeFile=/absolute/path/to/release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

然后执行：

```bash
DIALOGUE_ROUTE_KEYSTORE_PROPS=/absolute/path/to/release.properties ./gradlew :app:assembleRelease
```

环境变量中的相对路径及 `storeFile` 相对路径均以仓库根目录为基准，建议使用绝对路径；Windows properties 路径建议使用正斜杠。显式指定的配置文件不存在或缺少字段时，Gradle 配置阶段会报错，避免误生成未签名包。签名文件与密码不得提交。

## 扩展平台采集

每个平台独立一个文件：`WeChatAdapter.kt`、`QQAdapter.kt`、`XAdapter.kt`、`FeishuAdapter.kt`。共享标题识别和时间戳判断在 `ChatNodeHelpers.kt`，契约在 `ChatAppAdapter.kt`。

新增平台时实现 `ChatAppAdapter`，再加入 `ChatCaptureService.adapters`。必须保留返回值语义：

| 返回值 | 含义 |
| --- | --- |
| `null` | 当前不是聊天窗口，不触发 OCR |
| 空消息的 `ChatSnapshot` | 已识别为聊天窗口，但节点无正文，可进入 OCR |
| 非空消息的 `ChatSnapshot` | 正常采集，继续通用分析流程 |

飞书的气泡矩形由 `collectFeishuBubbleRects` 收集，截屏回调会重新读取，避免截图与节点位置错位。无障碍服务保留系统组件类名，业务代码统一位于 `io.github.liaong13.dialogueroute`。

## 检查与验证

日常修改先做路径级差异审查及 `git diff --check`。需要动态验证时可运行：

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

设备验收应覆盖聊天页和列表页识别、空消息 OCR、悬浮窗隐藏与恢复、回复填入后不发送。记录设备、系统、聊天应用版本和复现步骤；历史记录不能代替当前设备验证。

Python 校准工具见 [tools/jev/README.md](../tools/jev/README.md)。校准会调用远端接口并消耗额度，不属于默认静态检查。

## 上游资料

[archive/](archive/) 保存上游探针设计、旧验收标准、v1.3 计划和任务记录，仅用于理解历史背景。其中机器路径、发布步骤、测试结论和任务分工可能已经过期。

`site/` 和已有 APK 也来自上游，不是本 fork 的官网和新版本产物。LICENSE、NOTICE、贡献者名单保留原始归属。
