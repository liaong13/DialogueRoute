# 项目文档

更新日期：2026-09-24。当前应用版本为 `1.3 / versionCode 4`；V1.4 是多平台增强模式的开发目标。

| 文档 | 用途与证据边界 |
| --- | --- |
| [项目首页](../README.md) | 功能、使用、构建与上游来源 |
| [开发约定](../AGENTS.md) | 工作区、修改边界、静态检查与交付要求 |
| [开发指南](development.md) | Compose/MIUIX 入口、主题、构建、签名及平台扩展 |
| [当前开发状态](current-development-status.md) | 当前代码、历史设备验证、剩余验收和换机接续 |
| [悬浮窗功能恢复与紧凑界面](overlay-restoration-2026-09-24.md) | v1.3 功能对照、当前工作区修复、图标与未验机边界 |
| [2026-09-24 变更记录](daily-changes-2026-09-24.md) | 今天 5 次既有提交的统计、改动内容及文档整理范围 |
| [V1.4 多平台设计](v1.4-libxposed-multiplatform-design.md) | 当前实施方向；通用架构规划不等同于已实现能力 |
| [早期微信备用方案](v1.4-wechat-capture-plan.md) | 已被多平台设计取代的历史评估 |
| [2026-09-23 静态评审](project-review-2026-09-23.md) | 历史问题与验证记录，附后续状态说明；未全量重审 |
| [LICENSE](../LICENSE)、[NOTICE](../NOTICE) | MIT 许可、上游版权与 Dialogue Route fork 归属 |

## 上游归档

归档只补维护说明与当前入口，保留原始内容。旧任务中的机器路径、UI 技术选择、版本、测试和发布指令可能过期，不作为本 fork 的开发指令或验收证据。

- [探针规格](archive/probe_spec.md)
- [验收标准](archive/acceptance.md)
- [Jev 校准任务](archive/jev-task.md)
- [v1.3 计划](archive/v1.3-plan.md)
- [v1.3 验收清单](archive/v1.3-morning-checklist.md)

上游 `site/`、历史 APK、独立 Python 校准工具及旧 CHANGELOG/CONTRIBUTORS 已移除；原始归属仍见 [LICENSE](../LICENSE)、[NOTICE](../NOTICE) 与项目首页。新构建产物通过本仓库 Releases 分发，源码仓库不存放 APK。
