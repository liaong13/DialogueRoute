# 当前开发状态（2026-09-23）

本文件供换电脑后接续开发。以代码和新设备日志为准；这里记录的是截至本次提交的结果，后续微信、系统或 LSPosed 升级后需重新验证。

## 获取与构建

仓库：`git@github.com:liaong13/DialogueRoute.git`，提交目标分支：`main`。另一台电脑使用 `git clone` 或在已有工作区执行 `git pull --ff-only origin main`。本地安装 JDK 17 或 25、Android SDK Platform 37，配置仅属于本机的 `local.properties`，然后运行：

```bash
sh gradlew :app:assembleDebug :app:testDebugUnitTest
```

Gradle Wrapper 为 9.4.1，AGP 为 9.2.1，应用使用 AGP 内置 Kotlin。调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`；构建产物、`local.properties`、模型密钥和签名材料不入库。

## 已实现

- 同一 APK 包含普通应用及 libxposed API 102 模块；在设置页开启“使用 Xposed 模式”，框架中另行启用模块并勾选微信作用域。两处开关独立。
- 微信 8.0.78（`versionCode=3180`）主进程：`DialogueRouteModule` 在 `Application.attach` 后注册生命周期回调，再使用真实 `LauncherUI` 的 ClassLoader 安装聊天页 hook。这解决了模块日志显示已安装、回调却不执行的问题。
- `WeChatReader` 从当前活动会话的适配器读取最多 30 条文本消息及说话方，经受限 `XposedProbeProvider` 送入主应用；不遍历整库、不读取登录凭据。正文与联系人标题不写入日志。主应用复用判断、回复、知识库和悬浮窗流程，草稿填入代码只写输入框，不发送；已有非空草稿不会被覆盖。
- 主界面和设置页分别显示正文验证状态与探针状态。聊天页长按紫色“攻”悬浮球，点“核对本次采集”，可在悬浮窗查看最近 6 条并自行对照。Xposed 正文成功时无障碍悬浮球让位；无 Xposed 正文时仍可走原无障碍/OCR 路径。

## 本轮实测

- 设备 `2509FPN0BC`，Android 17 / SDK 37，微信 8.0.78（3180），LSPosed 运行 API 102。具体 LSPosed 发行版与构建号未记录。
- 从设备 APK 用 JADX 核对了 `ChattingUIFragment`、聊天上下文、消息适配器、文本消息模型与输入框接口。运行日志确认模块在微信主进程加载、从真实 Activity ClassLoader 安装、聊天页 hook 命中；应用收到了非空正文心跳，主页显示“Xposed 微信正文已验证”。
- 用户在一个测试会话中通过“核对本次采集”确认最近 6 条正文及“我/对方”方向一致。该确认只覆盖当时的文本会话，不代表所有聊天类型。
- 这台设备的微信截图内容区为黑屏，`uiautomator` 仅有根节点，因此无法通过截图/OCR 自动逐字比对。临时截图和 UI dump 已清理；真实聊天正文、标题与模型密钥未写入仓库。
- `:app:assembleDebug`、`:app:testDebugUnitTest`、`git diff --check` 通过；调试 APK 已覆盖安装。为避免测试触发模型请求，曾临时关闭自动分析，验收后已恢复为原来的开启状态；应用内 Xposed 开关保持开启。

## 尚未验收及下一步

1. 在专用测试会话中验收“分析当前对话”、判断与回复的两路异步回调、选择回复后的微信草稿填入，以及已有草稿时的复制回退。模型请求会使用设备上配置的服务；发送仍由用户手动完成。本轮未调用真实模型，也未验收草稿填入。
2. 验收会话快速切换、同文不同联系人、离开聊天页、关闭助手和进程重建时，旧判断/回复及填入命令不会落到新会话；覆盖空消息、长文本、群聊发言人和非文本消息。目前只有微信普通文本会话完成实测。
3. 依照[多平台设计](v1.4-libxposed-multiplatform-design.md)继续 QQ、X、飞书适配。每个平台须按目标版本重新反编译与验机，不复制微信混淆成员。
4. 更新 APK 后需要重启微信进程使模块代码重新加载。若状态显示“应用内开关未开启”，在应用设置中打开并点“保存全部设置”；框架勾选模块本身不会设置应用内开关。若仅显示探针命中，应检查聊天页和正文心跳，不能把模块加载等同于采集成功。

`docs/v1.4-wechat-capture-plan.md` 是早期评估，当前实施约束以多平台设计、此状态记录和 live tree 为准。
