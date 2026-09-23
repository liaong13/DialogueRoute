# 2026-09-24 变更记录

## 统计口径

按 Asia/Shanghai（UTC+08:00）统计 2026-09-24 00:00 起、截至本次文档提交前 HEAD `e7c5dd8` 的当前分支提交，共 5 次。前一日基线为 `f5046b2`，范围为 `f5046b2..e7c5dd8`；以下统计不包含本次文档提交，也不包含 9 月 23 日已提交的包名迁移、微信 Xposed 接入和设备验证。

| 提交 | 时间 | 主要内容 | 单次提交差异 |
| --- | --- | --- | --- |
| `2b110fc` | 01:00:35 | 删除上游站点，更新相关说明 | 12 文件，+2 / -1,308 |
| `23a7213` | 01:53:06 | 删除历史 APK，调整分发说明 | 4 文件，+3 / -3，另删除二进制 APK |
| `7489ba5` | 01:54:07 | 主界面迁移至 Compose + MIUIX | 22 文件，+1,704 / -1,515 |
| `97a456b` | 02:09:53 | 默认浅色，应用和悬浮窗主题同步 | 9 文件，+248 / -152 |
| `e7c5dd8` | 02:17:03 | 删除上游 Python 校准工具、清理忽略规则及来源说明 | 12 文件，+5 / -1,559 |

以基线到 HEAD 的净差异计，共 **43 个文件，文本新增 1,831 行、删除 4,406 行**。同一文件可能跨提交多次修改，所以不累加各提交文件数或行数作为净统计。删除的 APK 原大小为 25,781,877 字节；二进制内容不计文本行数。

## 界面与外观

- `MiuixActivity.kt` 成为唯一应用 Activity，通过页签承载主页、知识库、设置；设置和知识库内容分别在 `MiuixSettings.kt`、`MiuixKnowledge.kt`。删除旧 `MainActivity.kt`、`SettingsActivity.kt`、`KnowledgeActivity.kt` 和 `Insets.kt`，Manifest 同步入口。
- 新增 `MiuixUi.kt` 与 `MiuixGlass.kt`，提供共用卡片、控件、页签与玻璃效果。设置保留显式保存，并对未保存离页提供继续编辑/放弃提示；知识库继续支持笔记和联系人管理。
- Gradle 启用 Compose，加入 Compose 插件 2.4.0、BOM 2026.09.00、Activity Compose 1.13.0，以及 MIUIX UI/preference/icons 0.9.3。应用版本保持 `1.3 / versionCode 4`，SDK 与 ABI 配置未随 UI 迁移改变。
- `Prefs.themeMode` 保存浅色、深色或跟随系统；缺省及非法值为浅色。点击“保存设置”后，Compose 配色及系统栏响应偏好变化。
- `OverlayController.kt` 与新增的 `OverlayViews.kt` 继续使用 Android View，实现主题配色、卡片和按钮样式，以及偏好/系统配置变化监听；已经显示的视图可刷新颜色。回复仍只复制或填入，由用户手动发送。
- `XAdapter.kt` 仅将注释中的 `MainActivity` 改为宿主 Activity 的通用表述，没有新增 X 平台采集行为。

## 仓库与文档

今天已提交删除 `site/` 及其静态资源、`apk/jev-assistant-v1.3-release.apk`；新 APK 通过本仓库 Releases 分发。

本次整理开始时已有 README 精简，以及已暂存的 `CHANGELOG.md`、`CONTRIBUTORS.md` 删除，均予以保留。LICENSE、NOTICE 与 README 的上游归属保留。

整理期间，独立 Python 校准工具删除、`.gitignore` 规则清理和 `JevQuestions.kt` 来源注释更新已由另一项工作单独提交为 `e7c5dd8`，因此计入今天既有提交的统计。本次文档提交不重复纳入这些文件；题目集仍由应用维护，该注释修改不改变运行逻辑。

本次同步 README、AGENTS、开发指南、当前状态、多平台设计、早期方案、历史评审及 5 份归档的维护提示；新增本记录和文档索引。修正旧 UI 技术栈、设置按钮名称、悬浮球颜色描述及 Unix Wrapper 调用方式。历史方案/评审正文保留原始时点，不改写为今天的验证结论。

按用户追加要求同步 LICENSE 与 NOTICE：增加 liaong13 / Dialogue Route contributors 的 fork 版权与项目来源说明，保留原作者版权、MIT 许可正文和上游声明。

## 验证与待办

本记录依据本地 Git 提交、差异及当前 Gradle/Manifest/源码整理。路径级 `git diff --check` 通过；16 份现存文档（含 LICENSE/NOTICE）的空白及 62 个本地链接检查通过。本次未编译、未运行 JVM 测试、未验机、未调用模型接口。历史构建通过及微信文本核对仅覆盖 2026-09-23 的记录，不能作为今天 UI/主题改动的验收。

后续应单独验证三个页签及跳转、设置保存/放弃、知识库编辑、浅色/深色/跟随系统、系统栏与已展开悬浮窗的同步，再继续微信草稿填入、双路模型回调、会话隔离和其他平台适配。完整验收边界见 [当前开发状态](current-development-status.md)。
