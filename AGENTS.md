# Kagami · AI 协作约定

本文件是 AI 助手在本仓库工作时的常驻指令,请始终遵守。

## 提交纪律(最重要)

1. **每次提交前,检查文档是否需要随代码更新**。文档与代码保持同步是对项目的硬性要求:
   - 根 `README.md`:Project Status 里程碑、Tech Stack、API Overview、Getting Started;
   - `kagami-ui/README.md`:目录树、架构决策、命令;
   - `docs/learning-guide.md`:踩坑总表(文件引用勿过时)与阶段章节。
   - 判断标准:一个新会话的读者只看文档,能否正确理解项目现状?不能,就补。
2. **文档更新与对应代码放进同一提交**(纯文档变更可单独 docs 提交)。
3. **遇到值得记住的坑,当天补进** `docs/learning-guide.md` 的踩坑总表(域 / 坑 / 一句话教训 / 仓库位置)。
4. 提交信息用英文 Conventional Commits(`feat` / `fix` / `docs` / `polish` / `refactor`),body 说明"做了什么 + 关键决策为什么"。
5. 提交前必须完成验证:后端 `./mvnw compile`、前端 `npm run build`(在 `kagami-ui/` 下跑)

## 语言约定

- 代码、注释、提交信息:英文(沿既有风格);
- 根 README / 子 README:英文;
- `docs/learning-guide.md`:中文(面向学习者的路线图);
- UI 文案:`kagami-ui` 控制台英文。

## 项目现状

- 当前进度、架构决策、路线图:见根 `README.md` 的 Project Status 与 `docs/learning-guide.md`;
- 后端架构约束(会往返浏览器的数值 id 必须 < 2^53 或序列化为字符串、已应用的 Flyway 迁移不可变)记录在学习指南的踩坑总表,改动相关代码前先读。
