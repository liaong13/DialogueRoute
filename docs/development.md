# 开发指南

Codex 的仓库开发约定统一维护在根目录 [AGENTS.md](../AGENTS.md)。

文档同步日期：2026-09-25。完整入口见 [文档索引](README.md)，此前提交与统计见 [2026-09-24 变更记录](daily-changes-2026-09-24.md)。

## 工程入口

本项目是单模块 Android Gradle 工程，应用模块为 `app`，不涉及 AOSP System/Vendor 分仓。

- 构建配置：根目录和 `app/build.gradle.kts`。
- 配置与历史数据：`core/Prefs.kt`、`core/kb/`。
- 采集分发：`capture/ChatCaptureService.kt`。
- 模型调用：`jev/JudgeClient.kt`、`ReplyClient.kt`、`VisionClient.kt`。
- UI：`MiuixActivity.kt`、`MiuixSettings.kt`、`MiuixKnowledge.kt` 与 `overlay/`。
- 共用 UI：`MiuixUi.kt`、`MiuixGlass.kt`；Compose 悬浮窗入口为 `overlay/OverlayController.kt`、`overlay/OverlayCompose.kt`。

产品名为“对话攻略”，英文副标为“Dialogue Route”，包名为 `io.github.liaong13.dialogueroute`。包名与上游不同，因此会作为独立应用安装；旧包的配置和私有数据不会自动迁移。

## 构建与界面基线

当前 Gradle 配置为 `versionName=1.3`、`versionCode=4`，V1.4 是开发方案名称，尚未更新应用版本号。使用 JDK 17 或 25、SDK Platform 37、Gradle Wrapper 9.7.1、AGP 9.4.1；Java 源码/目标级别为 17，compileSdk 37、targetSdk 35、minSdk 30，仅包含 `arm64-v8a`。

主界面与悬浮窗已启用 Compose Material 3 和 Miuix 控件库，Compose 插件为 2.4.0、BOM 为 2026.09.00、Activity Compose 为 1.13.0；版本以根目录、应用 Gradle 配置和版本目录为准。Manifest 的唯一 Activity 仍为 `MiuixActivity`，主页、知识库、设置使用内部页签，外部入口通过 `EXTRA_TAB` 选择页签。旧 `MainActivity`、`SettingsActivity`、`KnowledgeActivity` 和 `Insets.kt` 已删除。

设置修改需点击“保存设置”；未保存时切换页签或返回会提示继续编辑或放弃修改。外观设置保存到 `Prefs.themeMode`、`Prefs.dynamicColor` 和 `Prefs.uiStyle`；明暗模式支持 `light`、`dark`、`system`，缺省或非法值回退浅色。Android 12 及以上可启用系统动态配色，旧系统使用应用配色。`Miuix` 风格使用 `miuix-ui-android:0.9.3` 的主题、底栏、侧栏、标签、输入框、按钮、开关和单选控件；`Material 3` 风格保留原控件路径。玻璃背景和部分业务卡片由共享 Haze 组件提供，两种风格共用。主界面、系统栏与 Compose 悬浮窗同步外观设置；悬浮面板使用系统跨窗口模糊能力并在不支持时回退半透明背景。窄屏 Material 3 底栏使用可拖动胶囊指示器，大屏（宽度不小于 840dp）使用侧栏；系统关闭动画时导航直接切换页面。主题回归需覆盖已展开面板、跟随系统、窗口隐藏后重开、大小屏切换，以及监听器释放。

macOS/Linux 使用 `sh gradlew :app:assembleDebug`；Wrapper 在 Git 中没有执行位。Windows 使用 `gradlew.bat :app:assembleDebug`。APK 输出为 `app/build/outputs/apk/debug/app-debug.apk`，SDK 路径由本机 `local.properties` 配置。

## Release 签名

未设置 `DIALOGUE_ROUTE_KEYSTORE_PROPS` 时，`sh gradlew :app:assembleRelease` 生成未签名 APK。

需要签名时，在仓库外创建 properties 文件，填入：

```properties
storeFile=/absolute/path/to/release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

然后执行：

```bash
DIALOGUE_ROUTE_KEYSTORE_PROPS=/absolute/path/to/release.properties sh gradlew :app:assembleRelease
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

## V1.4 多平台 libxposed 设计与现状

[当前开发状态](current-development-status.md)分开记录 2026-09-24 的代码状态、2026-09-23 的设备验证和剩余验收项；历史测试不代表新界面版本已验收。

[当前设计](v1.4-libxposed-multiplatform-design.md) 替代最初的微信只读备用方案。用户要求同一个 APK 同时是应用和 Xposed 模块，实施顺序为 QQ → 微信 → X → 飞书。当前 `:app` 已针对微信 8.0.78（3180）接入当前会话文本、主应用分析及草稿填入代码；Android 17 设备已验证模块加载、聊天页命中和非空正文心跳，用户确认一个测试会话最近 6 条正文及方向一致，草稿填入仍待端到端验证：

- `:app` 已声明 `api:102.0.0` 为 `compileOnly`，compileSdk 37，并打包 `META-INF/xposed/` 入口；框架 API 不进入 APK 运行时依赖，不混用 legacy API。
- 模块使用会话/消息模型、加载/更新回调、有界内部查询及必要的绑定/UI hook，支持当前会话屏外近期上下文、发送者及用户触发的草稿填入。
- 当前由 `XposedCaptureRuntime` 在应用进程中独立处理微信快照、分析和填入命令，不依赖无障碍服务启动；原无障碍适配器三态契约保留。后续多平台接入时再评估是否抽出通用采集协调器与填入路由。
- 后续若接入 libxposed service，只用于框架控制和非敏感配置；聊天和命令使用有调用身份校验的双向 Binder。注入代码使用宿主 UID，不等同于本应用 UID。
- 统一处理会话/账号隔离、请求代次、两路结果乱序、知识库关联、填入幂等、OCR 刷新和脱敏。主应用持有密钥，模型接口不在宿主进程调用。
- 不以“非侵入性”限制适配层，但保留用户手动发送和不操作支付的产品边界。真实类/方法及支持版本须取证，按四个平台逐项验收；探针不代表微信正文采集可用。

## 检查与验证

日常修改先做路径级差异审查及 `git diff --check`。需要动态验证时可运行：

```bash
sh gradlew :app:testDebugUnitTest
sh gradlew :app:assembleDebug
```

Windows 对应使用 `gradlew.bat :app:testDebugUnitTest` 和 `gradlew.bat :app:assembleDebug`。这些是验证入口，不表示本次文档整理已执行构建或测试。

设备验收应覆盖聊天页和列表页识别、空消息 OCR、悬浮窗隐藏与恢复、回复填入后不发送，以及三个页签、设置保存/放弃、知识库编辑和三种主题的同步。记录设备、系统、聊天应用版本和复现步骤；历史记录不能代替当前设备验证。

## 上游资料

[archive/](archive/) 保存上游探针设计、旧验收标准、v1.3 计划和任务记录，仅用于理解历史背景。其中机器路径、发布步骤、测试结论和任务分工可能已经过期。

上游独立 Python 校准工具已移除，不参与当前应用的构建或测试；历史文档中的工具路径与命令不再适用。旧脚本与标注样本可从 Git 历史恢复。后续如需模型质量校准，应以应用实际使用的 `JevQuestions`、模型配置和知识库上下文构造为准。

源码仓库已移除上游 `site/` 和历史 APK，新版本产物通过本仓库 Releases 分发。旧 `CHANGELOG.md`、`CONTRIBUTORS.md` 已随文档精简移除；[LICENSE](../LICENSE)、[NOTICE](../NOTICE) 与 README 的上游归属继续保留，本 fork 的当日记录在文档索引中维护。
