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

## V1.4 多平台 libxposed 设计与现状

[当前开发状态](current-development-status.md)记录本轮已验证的设备结果、剩余验收项和换机接续步骤。

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
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

设备验收应覆盖聊天页和列表页识别、空消息 OCR、悬浮窗隐藏与恢复、回复填入后不发送。记录设备、系统、聊天应用版本和复现步骤；历史记录不能代替当前设备验证。

Python 校准工具见 [tools/jev/README.md](../tools/jev/README.md)。校准会调用远端接口并消耗额度，不属于默认静态检查。

## 上游资料

[archive/](archive/) 保存上游探针设计、旧验收标准、v1.3 计划和任务记录，仅用于理解历史背景。其中机器路径、发布步骤、测试结论和任务分工可能已经过期。

源码仓库不再保留上游历史 APK，新版本产物通过本仓库 Releases 分发。LICENSE、NOTICE、贡献者名单保留原始归属。
