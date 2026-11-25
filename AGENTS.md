# Kagami · AI 协作约定

本文件是 AI 助手在本仓库工作时的常驻指令，请始终遵守。

## 1. 按需阅读路由（先定位，再动代码）

新会话严禁盲目全仓扫描，按任务类型只读对应入口：

- **后端业务 / API**：`README.md`（API Overview）+ 对应域包（`user` / `post` / `comment` / `security`）+ `common/exception/ErrorCode.java`
- **数据库变更**：`src/main/resources/db/migration/` 最新迁移脚本 + `docs/mock_data.sql`
- **前端页面 / 状态 / 样式**：`ui/README.md` + `ui/src/styles/tokens.css` + 对应 `stores/` / `views/`
- **查历史踩坑**：只读 `docs/learning-guide.md` 末尾的「踩坑总表」与「知识点地图」

## 2. 协作与变更原则

1. **聚焦任务**：只修改与当前任务直接相关的文件，不顺手重构无关代码。
2. **后端高频惯例**：Controller 直接返回裸 DTO（`ResultWrapperAdvice` 自动包裹 `R<T>` 信封）；业务异常统一抛 `BusinessException(ErrorCode.*)`；时间一律注入 `common/time/Clock`（禁用 `Instant.now()`）。
3. **关键架构红线（必须遵守）**：
   - **ID 精度**：会往返浏览器的数值 ID 必须 `< 2^53`（实体配 `@TableId(type = IdType.AUTO)` 对齐 `bigserial`）或序列化为字符串。
   - **数据库迁移**：已应用的 Flyway 脚本（`src/main/resources/db/migration/V*.sql`）严格不可变（含注释）；结构变更必须新增版本化迁移，并同步实体、Mapper 与 `docs/mock_data.sql`。
   - **安全与配置**：不得提交私钥或敏感凭据；调整鉴权、`@AnonymousAccess` 或 `@RateLimit` 策略须显式说明影响。
   - **前端样式与组件**：`ui/` 零第三方组件库；禁止在组件中硬编码 hex 或 `color-mix()`，颜色只消费 `ui/src/styles/tokens.css` 的 OKLCH 语义变量。
   - **前端交互与媒体边界**：`body` 全局 `user-select: none`（配合双击展开播放器手势），阅读正文区域必须加 `.selectable` 白名单，可交互控件按需 `@dblclick.stop` 防冒泡；文章读写走 `ui/src/data/sources/` 门面；大媒体文件托管于 R2，不提交到 Git（`ui/public/audio/` 已忽略）。
4. **提交边界**：除非用户明确要求执行 Git 提交，否则完成修改与验证后输出变更摘要与建议的提交信息，由用户确认提交。

## 3. 验证与文档同步收尾清单

1. **必跑验证**（Windows 后端命令用 `mvnw.cmd`）：
   - 后端：`./mvnw compile`；改动业务逻辑或接口时运行相关测试（`./mvnw test`，当前仅 `CommentApiControllerTest` 有集成测试且依赖本地 Docker）。
   - 前端（在 `ui/` 目录下）：`npm run build`（含 `vue-tsc -b` 类型检查与 Vite 构建）。
2. **文档同步自检**（按需更新，不强求每次微调都改全部文档）：
   - 增删改了 API、错误码、依赖、启动方式或里程碑？→ 更新根 `README.md`
   - 增删改了 `ui/` 目录结构、Store、Token 或交互约定？→ 更新 `ui/README.md`
   - 遇到非显而易见的坑或架构权衡？→ 追加 `docs/learning-guide.md` 踩坑总表（域 / 坑 / 一句话教训 / 仓库位置），确保路径有效。
3. **提交信息规范**：英文 Conventional Commits（`feat` / `fix` / `docs` / `polish` / `refactor` / `test`），body 说明“做了什么 + 关键决策为什么”。

## 4. 语言约定

- **代码、注释、提交信息**：英文。
- **根 `README.md` 与 `ui/README.md`**：英文。
- **`docs/learning-guide.md`**：中文。
- **`ui/` 公开站文案与用户可见提示**：中文。
