# AGENTS.md

## 项目与范围

- 默认用简体中文沟通，代码、命令和日志关键字保留原文。
- 本仓库是“对话攻略（Dialogue Route）”，基于上游 Jev 聊天助手的独立 fork。应用包名为 `io.github.liaong13.dialogueroute`。
- 这是单模块 Android Gradle 应用，不是 AOSP System/Vendor 工程；不要套用平台分仓排查流程。
- 技术栈是 Kotlin + 传统 Android View，无 Compose 或 Web 前端构建链。版本以 Gradle 文件为准；当前使用 JDK 17、compileSdk 37、targetSdk 35、minSdk 30，仅打包 `arm64-v8a`。
- 开始任务先读 [README.md](README.md) 和 [开发指南](docs/development.md)，检查 `git status --short` 与目标文件已有 diff。新增但未跟踪的源码也是当前工作区的一部分，不能只审查 `git diff`。
- 用户只要求分析、检查或评审时，默认只读业务代码；明确要求修复时才修改。保留已有本地改动，使用最小补丁，不自动提交、推送或清理工作区。

## 代码入口

业务源码根目录为 `app/src/main/java/io/github/liaong13/dialogueroute/`。

| 路径 | 职责 |
| --- | --- |
| `MainActivity.kt`、`SettingsActivity.kt`、`KnowledgeActivity.kt` | 主界面、模型配置、知识库编辑 |
| `capture/ChatCaptureService.kt` | 无障碍采集分发、分析调度、回复填入 |
| `capture/*Adapter.kt`、`capture/ChatNodeHelpers.kt` | 各聊天平台节点解析与共享辅助逻辑 |
| `capture/ocr/` | 截屏、坐标映射、本地 ML Kit OCR |
| `overlay/OverlayController.kt` | 悬浮窗、判断结果、回复选项 |
| `jev/` | 判断、回复、视觉客户端及 HTTP 封装 |
| `core/Prefs.kt`、`core/ChatModels.kt` | 配置与数据契约 |
| `core/kb/` | 笔记、联系人、聊天历史与上下文构造 |

- 清单为 `app/src/main/AndroidManifest.xml`，资源位于 `app/src/main/res/`。
- 无障碍入口有意保留 `app/src/main/java/com/google/android/accessibility/selecttospeak/SelectToSpeakService.kt`；它继承业务服务。不要随业务包名迁移而直接重命名，修改时同时核对 Manifest、配置 XML 和主页权限检测。
- JVM 测试位于 `app/src/test/java/io/github/liaong13/dialogueroute/`。
- `tools/jev/` 是独立 Python 校准工具，入口和依赖见其 README，不参与 APK 构建。
- `docs/archive/` 保存上游历史资料。不能把其中任务分工、机器路径、设备测试结论、发布渠道作为当前开发指令或本 fork 已验证结果。APK 构建产物不入库，新版本通过本仓库 Releases 分发。保留 LICENSE、NOTICE 和原作者归属。

## 必须保持的行为边界

- 当前已有无障碍与 OCR 路径，以及微信 8.0.78（3180）的 Xposed 文本采集实现；具体已验证范围见[当前开发状态](docs/current-development-status.md)。V1.4 按[多平台 libxposed 方案](docs/v1.4-libxposed-multiplatform-design.md)继续推进，优先 libxposed，按 QQ → 微信 → X → 飞书实施，以功能效果为先；允许有范围的宿主会话/消息模型 hook、当前会话内部查询、必要的参数/结果适配及用户触发的草稿填入，不以“非侵入性/只读 View”限制实现。正文获取限当前活动会话，不导出整库或登录凭据；未列为已验证的功能不得宣称验机通过。
- 回复只复制或填入，发送必须由用户手动完成；不操作转账、红包、收款。
- 平台适配器的三态契约：`null` 表示非聊天页；空消息的 `ChatSnapshot` 表示聊天页无正文、可触发 OCR；非空消息表示正常采集。不要用普通搜索框代替聊天页判据。
- 无障碍平台实现 `ChatAppAdapter` 并注册到 `ChatCaptureService.adapters`，平台节点规则留在对应适配器；后续 libxposed 平台实现独立 `HostPlugin`，不强行依赖无障碍节点。宿主内部类/方法必须按目标版本取证，不凭旧注释复制。
- 会话身份、内容去重和请求生命周期应分开处理。修改异步流程时，检查切换会话、离开聊天页、关闭助手、销毁服务，以及判断/回复/OCR 回调乱序；填入前核对目标仍是原会话。
- 截屏保留限频、失败退避、超时恢复和资源释放；节点矩形是屏幕坐标，截图可能是带原点偏移的窗口坐标，不混用。
- 当前自动 OCR 使用随包 ML Kit 中文模型。`VisionClient` 有设置页连通测试，不能仅凭配置项存在就宣称已接入采集流程。
- 判断、回复、视觉的地址、密钥、模型分别配置；调整回退逻辑时核对目标服务，不能把一家的密钥无条件发到另一家。不要因审查或静态验证调用真实模型接口。
- 联系人由用户创建；聊天历史默认关闭。知识库在应用私有 `filesDir/kb` 中，修改存储时检查写入失败、缓存一致性、去重和联系人隔离。
- 密钥、签名材料、真实聊天正文、联系人标题不得进入 Git 或日志。错误响应也可能包含敏感内容，应脱敏；现有不符合之处需报告，不能据此扩大记录范围。
- 当前密钥使用应用私有 SharedPreferences，知识库使用 JSON 文件，不能宣称已加密。聊天与选中的知识库上下文会发送到所配置的模型服务。

## 检查与验收

- 默认只做静态检查：目标路径的差异审查、`git diff --check -- <paths>`，必要时 XML/JSON 解析、Python AST 和脚本语法检查。未跟踪文件需单独检查空白和内容。
- 不默认编译、安装 APK、改变设备设置或运行收费校准。用户要求编译或动态验证后，按改动范围选择命令，勿将测试命令存在写成测试已通过。
- macOS/Linux 可用以下命令；使用 `sh` 调用 Wrapper 不依赖当前文件的可执行位：

```bash
sh gradlew :app:testDebugUnitTest
sh gradlew :app:assembleDebug
sh gradlew :app:lintDebug
```

- Windows 使用 `gradlew.bat`。SDK 路径通过本机 `local.properties` 配置，不写死开发者目录。
- Release 签名仅通过 `DIALOGUE_ROUTE_KEYSTORE_PROPS` 指定仓库外配置；完整步骤见开发指南，不写入密码或签名文件。
- 功能验证重点覆盖：聊天/列表页识别、同内容不同联系人、分析期间切换会话、新消息到达、两路结果反向完成、OCR 空结果/失败重试、关闭助手后回调、填入但不发送。
- 交付说明真实修改文件、已执行检查、发现的问题与剩余风险；明确标注“静态推断”“未编译”“未验机”。当前代码和日志优先于历史注释；不要臆造构建通过或运行时修复结果。
