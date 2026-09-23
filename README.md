# 对话攻略 · Dialogue Route

一款带有 Galgame / Visual Novel 选项感的 Android 聊天决策助手。它读取当前聊天上下文，判断对方意图、风险和关系走向，再给出多条回复选项；选择哪条、是否发送始终由用户决定。

本项目由 [liaong13](https://github.com/liaong13/jev-chat-jarvis) 基于 [jev-chat/jev-chat-jarvis](https://github.com/jev-chat/jev-chat-jarvis) 独立开发，不代表上游官方版本。

通过无障碍服务读取当前聊天界面，结合模型分析生成候选回复，支持复制或填入输入框，发送由用户手动完成。

## 功能与限制

- 微信、QQ、X、飞书的平台采集适配器；节点无法提供正文时支持截屏和本地 OCR。
- 判断、回复、视觉三路模型接口可分别配置。
- 悬浮窗以“剧情判断 + 回复选项”的方式展示分析结果，本地知识库和联系人为分析补充上下文。
- Android 11（API 30）及以上；当前 APK 配置仅包含 `arm64-v8a`。
- 聊天软件版本、界面语言和系统无障碍限制会影响采集效果；上游历史测试记录不代表本 fork 已完成设备验证。
- OCR 只能识别可见区域，受保护窗口可能无法截屏；群聊、发言人识别和模型判断可能有误。

## 使用

1. 按下方说明构建并安装 APK。
2. 在设置中配置模型接口地址、API Key 和模型，使用连通测试检查配置。
3. 按主页指引开启无障碍、悬浮窗权限，并根据设备设置后台运行权限。
4. 打开聊天窗口，通过悬浮窗查看关系走向和回复选项，复制或填入选中的回复。

密钥保存在应用私有的 SharedPreferences 中，目前不是加密存储。分析时，聊天内容及启用的知识库上下文会发送给所配置的模型服务；本地 OCR 使用随包提供的 ML Kit 模型。聊天历史默认关闭，启用后保存在应用私有目录。请自行确认所配置服务的数据处理方式。

## V1.4 Xposed 进度

计划基于 libxposed 增加多平台增强模式，按 **QQ → 微信 → X → 飞书** 推进。同一个“对话攻略”APK 同时提供普通应用和 Xposed 模块入口；在兼容的 KernelSU + Zygisk Next + LSPosed 环境中获取当前会话的结构化消息、更新及发送者，并支持用户触发的宿主输入框填入。发送仍由用户手动完成。

增强模式需在框架和应用设置中分别启用；已适配平台优先使用 Xposed 数据，无障碍与可用的本地 OCR 保留为兼容路径。微信 8.0.78（3180）的当前会话文本读取、分析、悬浮窗及草稿填入已接入代码；Android 17 测试机已验证模块加载、聊天页命中、非空正文心跳和主页“已验证”状态。截图与无障碍层在该机上无法取得微信正文；用户已通过悬浮窗“核对本次采集”确认一个测试会话最近 6 条正文及“我/对方”方向一致。草稿填入与模型结果尚未端到端验证。其他消息类型和平台仍待适配。API 102、构建要求、四平台适配和阶段验收见 [V1.4 多平台 libxposed 方案](docs/v1.4-libxposed-multiplatform-design.md)。

换电脑继续开发时先看[当前开发状态](docs/current-development-status.md)。

## 构建

环境：JDK 17 或 25、Android SDK Platform 37；Gradle 使用仓库自带的 Wrapper。SDK 路径通过 Android Studio 或本机 `local.properties` 配置。

```bash
git clone https://github.com/liaong13/DialogueRoute.git
cd DialogueRoute
./gradlew :app:assembleDebug
```

Windows 使用 `gradlew.bat :app:assembleDebug`。输出为 `app/build/outputs/apk/debug/app-debug.apk`。

Release 签名配置及开发入口见 [开发指南](docs/development.md)。仓库已有的 `apk/jev-assistant-v1.3-release.apk` 是继承自上游的历史产物，不包含本 fork 后续修改；新版本产物应通过本仓库 Releases 分发。

应用包名为 `io.github.liaong13.dialogueroute`。它与上游包是两个独立应用，系统不会迁移上游包中的密钥、设置或聊天历史。

## 项目结构

| 路径 | 用途 |
| --- | --- |
| `app/` | Android 应用，Kotlin + 传统 View |
| `app/src/main/java/io/github/liaong13/dialogueroute/capture/` | 平台适配器、无障碍服务、保活与 OCR |
| `app/src/main/java/io/github/liaong13/dialogueroute/jev/` | Jev 模型客户端、题目集与 HTTP 封装 |
| `app/src/main/java/io/github/liaong13/dialogueroute/overlay/` | 悬浮窗与回复选项 UI |
| `app/src/main/java/io/github/liaong13/dialogueroute/core/` | 配置、数据模型、知识库和上下文 |
| `tools/jev/` | Python 题目校准工具 |
| `docs/` | 当前开发说明与上游历史文档 |
| `site/` | 继承的上游宣传站点，见其目录说明 |

## 来源与许可

基于 Jev 聊天助手二次开发。原作者及贡献者信息见 [CONTRIBUTORS.md](CONTRIBUTORS.md)，上游版本记录见 [CHANGELOG.md](CHANGELOG.md)。

保留原项目 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。本 fork 不沿用上游的赞助、捐赠、交流群和官方发布渠道。
