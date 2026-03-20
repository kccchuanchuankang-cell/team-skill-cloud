---
name: bitbucket-code-review
description: >-
  Fetches Bitbucket Cloud PR diffs via bundled Python scripts, then performs AI code review
  against team Java coding standards and error-code rules. Use whenever the user pastes a
  Bitbucket pull-request URL, asks to review PR #N, wants a prereview before formal review,
  or says code review / CR / prereview / 代码审查 / 预审 — including Java backend changes
  that need encoding, architecture, and ErrorCategory/ErrorCode checks.
---

# Bitbucket PR 代码审查（团队规范）

本 skill **自包含**：规范文档在 `references/`，拉取脚本在 `scripts/`。  
**Skill 根目录**指安装或 `openskills read` 展开后的 `bitbucket-code-review/` 文件夹（下文记为 `{SKILL_ROOT}`）。

## 何时使用

- 用户提供 `bitbucket.org/.../pull-requests/N` 或要求审查指定 PR。
- 需要按团队文档做 **编码规范 + 错误码 + 架构/设计** 双层审查（不可只做架构）。

## 硬规则

1. **脚本稳定性**：`scripts/fetch_pr_diff.py` 与 `scripts/bitbucket_pr_reader.py` 为共享工具，**不要在审查任务中擅自改写**；认证失败时排查 Token、用户名、URL 与权限，或交由维护者修改 skill 仓库。
2. **Diff 中文乱码**：工具输出里中文显示异常多为读取端 UTF-8 展示问题，**不要**当作业务代码编码缺陷上报。
3. **测试代码**：对 `src/test/**` 按指南跳过风格类检查（见指南）。
4. **审查顺序**：先做 **编码规范**，再做 **错误码**，再做 **架构与设计**；结论必须基于 diff 中 **`+` 行** 等事实，避免误报（如 diff 已有 `@Override` 仍报缺失）。
5. **每次执行前** 阅读 `references/CODE-PREREVIEW-GUIDE.md` 与两份规范（团队会持续更新）。

## Bundled 路径一览

| 用途 | 路径（相对 `{SKILL_ROOT}`） |
|------|-----------------------------|
| 流程与报告结构 | `references/CODE-PREREVIEW-GUIDE.md` |
| Java 编码规范 | `references/CODE-STANDARDS.md` |
| 错误码规范 | `references/ERROR-CODE-STANDARDS.md` |
| PR diff 拉取 | `scripts/fetch_pr_diff.py` |
| 可选提示词骨架 | `assets/code-prereview-template.md` |

## 环境与依赖

- Python 3，依赖：`pip install -r scripts/requirements.txt`（在 `{SKILL_ROOT}` 下执行）。
- 认证：`BITBUCKET_API_TOKEN`、`BITBUCKET_USERNAME`（邮箱），或命令行 `--api-token` / `--username`（详见指南）。

## 工作流

### 1. 拉取 PR 材料

在任意**可写工作目录**（建议新建空目录，便于收集输出）中执行：

```bash
cd {SKILL_ROOT}/scripts
python fetch_pr_diff.py <PR_URL>
```

脚本会在**当前工作目录**下创建形如 `{repo_slug}_PR{id}_{YYYYMMDDHHmm}/` 的文件夹，内含 `*_diff.txt`、`*_info.json`。

### 2. 分析

严格按 `references/CODE-PREREVIEW-GUIDE.md` 中的顺序与清单：

1. 编码：`references/CODE-STANDARDS.md`（魔法值、`@Override`、空指针、集合、日志、BigDecimal 等）。
2. 错误处理：`references/ERROR-CODE-STANDARDS.md`（适用时）。
3. 架构：职责、分层、复杂度、兼容性、设计一致性。

### 3. 交付物

在与 diff 同目录下生成 `*_review.md`，**UTF-8**，章节与严重级别遵循 **CODE-PREREVIEW-GUIDE** 中的报告模板（含 CRITICAL / IMPORTANT / MINOR / DESIGN、合规摘要、P0/P1/P2、清单、页脚等）。

## 严重级别速查

- **CRITICAL**：强制规范违反、安全/运行时风险、破坏 API/RPC 兼容。
- **IMPORTANT**：可维护性、建议性规则。
- **MINOR**：风格、可选清理。
- **DESIGN**：职责/分层/拆分（按指南）。

## 脚本失败时

记录 HTTP 401/404、认证与 URL；**不要**为通过审查而临时改脚本逻辑——引导用户检查 Token、用户名、仓库访问权。

## 指南中的路径说明

`CODE-PREREVIEW-GUIDE.md` 内若出现与「当前目录」或旧仓库布局相关的描述，**一律以本 skill 的 `references/`、`scripts/` 为准**（见上表）。
