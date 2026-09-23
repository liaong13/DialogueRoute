# Jev 校准工具

使用 Python 3.10+，依赖标准库。模型密钥通过环境变量 `OPENROUTER_API_KEY` 提供，不写入源码或报告。

在仓库根目录执行：

```bash
python3 tools/jev/demo_meme.py
python3 tools/jev/calibrate.py --limit 5
```

去掉 `--limit` 会运行完整标注集。脚本会调用模型接口并消耗额度，运行前确认密钥和服务配置。

- `jev_client.py`：请求封装。
- `questions.py`：判断题、排序题与上下文构造。
- `fixtures/labeled_set.json`：校准样本。
- `calibrate.py`：输出指标及 `report/` 下的校准结果。
- `probe_background_field.py`：上下文字段实验工具。

原始任务单已归档到 [docs/archive/jev-task.md](../../docs/archive/jev-task.md)，不作为当前开发指令。
