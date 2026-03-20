---
id: starry.code-review.prereview
title: Code Review - prereview
description: Helping you prereview the code by yourself before the formal code review.
intent: support
model_constraints:
  family: Claude Sonnet 4.5
  max_tokens: 512
  temperature: 0.0
tags: [support,cr]
owner: leven.jin
reviewed: 2026-01-06
---
作为一名Java后端工程师，现在你需要对一个pull request进行审查代码，pull request地址为{{abc}}，api-token为{{token}}，username为{{user}}。

审查代码时请严格参考本 skill 内 `references/CODE-PREREVIEW-GUIDE.md` 文档来进行生成（以 `npx openskills read` 展开后的 skill 根目录为准）。