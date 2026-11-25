# Kagami 架构笔记与踩坑指南

> 本文记录**当前仓库**（Spring Boot 后端 + Vue 3 公开站 `ui/`）的核心架构决策、演进脉络与真实踩坑记录。面向人类复盘与 AI 按需查坑（AI 开发时可直接跳至末尾「踩坑总表」与「知识点地图」）。

---

## 系统全景

```text
┌──────────────────┐        ┌──────────────────────────────────────┐
│ 浏览器 / 前端     │        │       Spring Boot 后端 (:8080)        │
│ ui/ (:5173)      │  JWT   │  ├─ /api/register /login /me         │
│ (当前读本地 MD，  ├──────► │  ├─ /api/posts/**  公开读(匿名+限流)  │
│  后续接真实 API)  │        │  ├─ /api/manage/posts/** 管理(JWT)    │
└──────────────────┘        │  ├─ /api/**/comments 评论(读匿名/写JWT)│
                            │  └─ ResultWrapperAdvice 统一信封      │
                            └──────────────┬───────────────────────┘
                                           │ MyBatis-Plus / Flyway
                            ┌──────────────▼───────────────────────┐
                            │        PostgreSQL 18.4 (:5432)       │
                            │   users / posts / tags / post_tags   │
                            │            / comments                │
                            └──────────────────────────────────────┘
```

### 核心选型取舍

| 选型 | 理由 | 放弃的方案 |
| --- | --- | --- |
| `bigserial` 主键 | ID 经 JSON 往返浏览器，必须落在 JS 安全整数（`< 2^53`）内 | MyBatis-Plus 默认雪花 ID（踩坑 #1） |
| 无状态 RS256 JWT | 前后端分离、服务端无会话状态、公私钥分离 | HttpSession |
| 显式 `/publish` 端点 | 防止编辑保存时误触发布；首次发布经 `Clock` 写 `published_at` 且幂等 | `PUT` 请求携带 `status` |
| 原始 Markdown 入库 | 后端不背解析器，渲染权交给前端，换渲染器不动数据 | 后端存渲染后 HTML |
| 原生控件 + OKLCH Token | 页面精简，避免组件库“后台脸”与自定义视觉对抗 | naive-ui / Vuetify 等重型组件库 |

---

## 模块设计要点

### 1. 基础骨架（`common/`、`data/`）
- **统一响应信封**：Controller 返回裸 DTO，`ResultWrapperAdvice` 自动包 `R<T>{code, msg, data}`；`ErrorCode` 按首位分域（`1xxxx` 用户、`2xxxx` 内容/评论、`9xxxx` 系统）。
- **单一时间写入方**：业务时间统一注入 `common/time/Clock`，审计字段由 `AuditMetaObjectHandler` 填充，数据库不写触发器。
- **异常收口**：`BusinessException` 与框架校验/系统异常统一在 `GlobalExceptionHandler` 处理。

### 2. 用户与安全（`security/`、`user/`）
- **声明式匿名放行**：`@AnonymousAccess` 标注类或方法，`AnonymousUrlCollector` 启动时收集并交由 `SecurityConfig` 放行，新增公开接口零配置改动。
- **防枚举与密码**：登录失败统一提示“用户名或密码错误”；BCrypt（强度 10）存储哈希；仅 `users.status = 0` 允许登录。
- **限流**：`@RateLimit` + Bucket4j 按“路由模式 + 客户端 IP（支持 `X-Forwarded-For`）”限流；当前为 Caffeine 本地桶（`kagami.rate-limit.backend: local`），抽象了 `BucketProvider` 以便后续无缝切 Redis。

### 3. 内容模型（`post/`、Flyway `V2`–`V3`）
- **`slug` 可空唯一**：PG 唯一索引允许多个 `NULL`，无 slug 文章走 ID 访问；服务层拒绝纯数字 slug（避免与 ID 命名空间冲突）。
- **分页与防 N+1**：必须显式注册 MyBatis-Plus 分页插件（`MybatisPlusConfig`，依赖 `mybatis-plus-jsqlparser`）；列表查询为 `1 次分页 + 1 次 IN 查作者 + 1 次 IN 查标签`。
- **增量扩展（`V3`）**：`pinned`、字数、阅读时长在写入时一次算好；置顶索引 `(status, pinned desc, published_at desc)` 与 `ORDER BY` 对齐。

### 4. 两层评论与集成测试（`comment/`、Flyway `V4`、`src/test/`）
- **两层盖楼**：`parent_id` 为 `NULL`（顶层）或指向同文章的顶层评论（服务层强校验），无递归查询；自引用外键 `on delete cascade` 自动级联删除回复。
- **轻审核**：仅登录用户可评论，无待审状态；文章作者可隐藏/恢复（`status` 0/1）。隐藏顶层评论时回复在组装层一并隐藏，恢复即还原，不改回复状态。
- **真实库集成测试**：`CommentApiControllerTest` 使用 Testcontainers 拉起 `postgres:18.4` + Flyway 全量迁移 + `@Transactional` 自动回滚；测试内直接用 `JwtTokenProvider` 签发 Token 以避开登录限流。

### 5. 公开站前端（`ui/`）
- **单色相 OKLCH Token**：`ui/src/styles/tokens.css` 以 `--hue: 286` 驱动全站明暗双主题，组件零硬编码颜色。
- **常驻黑胶播放器**：`App.vue` 并列常驻 `HomeView` 与 `VinylPlayer`；通过纯 CSS `calc()` 坐标实现全屏舞台与顶栏圆球的平滑形变，媒体流托管于 Cloudflare R2（`ui/public/playlist.json`）。
- **可替换数据层**：本地 Markdown（`ui/src/content/posts/`）经 `ui/src/data/sources/local.ts` 预加载，对组件只暴露 `useArticlesStore()`，后续接入 `/api/posts` 仅替换数据源。

---

## 踩坑总表（按域速查）

| 域 | 坑 | 一句话教训 | 仓库位置 |
| --- | --- | --- | --- |
| 后端 | MP 默认雪花 ID（~2^63）超出 JS 安全整数，JSON 到浏览器被舍入导致按 ID 查询/发布失败（含 JWT `user_id`） | 会往返浏览器的数值 ID 必须 `< 2^53`（`@TableId(type = IdType.AUTO)` 对齐 `bigserial`）或序列化为字符串 | `Post.java`、`JwtTokenProvider.java` |
| 后端 | MP 分页拦截器未注册时不报错，静默退化为 `total = 0` | 全局基础设施一次性配齐；MP 3.5.9+ 分页依赖拆在 `mybatis-plus-jsqlparser` | `MybatisPlusConfig.java`、`pom.xml` |
| 后端 | `LambdaQueryWrapper` 条件参数急切求值，`.like(cond, col, keyword.trim())` 在 `keyword` 为 null 时 NPE | 先归一化参数，再传入 wrapper | `PostService.pageMine` |
| 后端 | `MetaObjectHandler` 不覆盖纯 wrapper 更新 | wrapper-only update 手动 `set(updatedAt)` | `PostService.update` |
| 后端 | LIKE 查询未转义通配符，`100%` 命中任意后缀 | 用户输入进 LIKE 前转义 `%`、`_`、`\` | `PostService.escapeLike` |
| 后端 | 反向代理下限流桶按代理 IP 共享 | 反向代理必须转发 `X-Forwarded-For` | `RateLimitInterceptor.java` |
| 后端 | Boot 4 测试分包：`@AutoConfigureMockMvc` 移至 `spring-boot-starter-webmvc-test`，Jackson 3 包名为 `tools.jackson`，Testcontainers 1.x 需显式版本 | 升级 Boot 4 注意测试 starter 拆分与 Jackson 3 包路径 | `pom.xml`、`CommentApiControllerTest.java` |
| 后端 | 集成测试无 `@Transactional` 回滚导致用例间数据污染 | 测试类加 `@Transactional` 自动回滚隔离 | `CommentApiControllerTest.java` |
| 后端 | `@Valid` 校验错误映射进 `R.data` | 前端字段标红依赖该结构，勿随意改动 | `GlobalExceptionHandler.java` |
| 数据库 | 已应用的 Flyway 迁移修改任意字节（含注释）导致 checksum mismatch 启动失败 | 已应用的 `V*.sql` 严格不可变，变更一律新增迁移 | `src/main/resources/db/migration/` |
| 数据库 | 造数时无关联 `LATERAL` 只求值一次；同一 `SELECT` 两次 `random()` 产生脏数据 | 子查询须引用外层列；相关字段从同一随机源推导 | `docs/mock_data.sql` |
| 前端 | 全局双击手势下，双击空白处触发浏览器选词高亮无关文本 | `body` 默认 `user-select: none`，仅对阅读正文加 `.selectable` 白名单 | `ui/src/style.css` |
| 前端 | `100vw` 含滚动条宽度而 `fixed` 布局视口不含，停靠坐标偏出视口 | 在 `inset: 0` 的 fixed 容器内用 `left: calc(100% - ...)` | `ui/src/components/VinylPlayer.vue` |
| 前端 | 形变两端含 `auto` 时无法 CSS 插值导致瞬移 | 两端用同一组可插值 `calc()` longhand，旋转动画放子元素避免争抢 `transform` | `ui/src/components/VinylPlayer.vue` |
| 前端 | `pointer-events: none` 不阻止事件冒泡，子控件双击误触祖先手势 | 可交互子控件显式加 `@dblclick.stop` | `ui/src/components/VinylPlayer.vue` |
| 前端 | 形态切换或飞行扫过静止指针时，事件驱动 hover 状态过期/误弹悬浮条 | 形态切换重置状态、飞行期抑制监听，落定后用 `matches(':hover')` 复核 | `ui/src/components/VinylPlayer.vue` |
| 前端 | 重构模板遗漏 `ref` 绑定（`progressElRef`），拖动/seek 静默失效 | 改模板逐项核对 ref；静默 no-op 需靠运行时 DOM 验证定位 | `ui/src/components/VinylPlayer.vue` |
| 前端 | 展开态全屏舞台背后页面仍可滚动，且 `scrollbar-gutter` 露出异色条 | 滚动锁挂根元素 `html.player-expanded { overflow: hidden; background: var(--color-intro-bg) }`，配合 `scrollbar-gutter: stable` 零回流 | `ui/src/stores/player.ts`、`ui/src/style.css` |
| 工程化 | npm 全局 `omit=dev` 导致 `vue-tsc` 缺失；TS 6 弃用 `baseUrl` | 缺构建工具先查 npm 配置（`--include=dev`）；TS 6 `paths` 必须写 `./` 相对路径 | `ui/package.json`、`ui/tsconfig.app.json` |

---

## 知识点地图（仓库锚点）

- **后端**：统一信封（`common/web/ResultWrapperAdvice.java`）、异常与分域错误码（`common/exception/`）、审计与时钟（`data/handler/`、`common/time/`）、JWT 与安全链（`security/`）、声明式匿名（`security/config/AnonymousUrlCollector.java`）、限流（`security/ratelimit/`）、文章与分页（`post/`、`data/config/MybatisPlusConfig.java`）、两层评论与集成测试（`comment/`、`src/test/java/im/kagami/comment/`）。
- **数据库**：`bigserial`、应用侧 `timestamptz`、可空唯一索引（`V2`）、增量回填与排序索引（`V3`）、自引用级联（`V4`）、造数脚本（`docs/mock_data.sql`）。
- **公开站 (`ui/`)**：单色相 OKLCH Token（`ui/src/styles/tokens.css`）、常驻唱片形变与 R2 歌单（`ui/src/components/VinylPlayer.vue`、`ui/public/playlist.json`）、本地 Markdown 可替换数据源（`ui/src/data/`）、详情切换与滚动恢复（`ui/src/views/HomeView.vue`、`ui/src/views/ArticleDetailView.vue`）、Pinia 状态（`ui/src/stores/`）。

---

## 后续演进方向

1. **测试补齐**：补充 `AuthController`、`UserController`、`PostPublicController`、`PostManageController` 与限流的单测/集成测试。
2. **CI 与部署**：GitHub Actions 跑 `mvnw verify` + `ui` 的 `npm run build`；`compose.yaml` 补后端服务编排。
3. **前后端联调**：`ui/` 引入路由、接通导航/搜索、将 `src/data/sources/` 切换至后端真实 API。
4. **功能与性能**：PG 全文检索、浏览量/点赞、限流与列表缓存切 Redis。
