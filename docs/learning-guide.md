# Kagami 学习指南 —— 如果从零由你来做一遍

> 这份文档不是使用说明,而是一份**重建路线图**:假设这个项目不存在,你要从空目录开始把它做出来,应该按什么顺序做、每一步怎么设计、会用到哪些知识、以及——最重要的——**每一步真实踩过的坑**(全部来自本仓库的实际开发过程,不是编造的教科书警告)。
>
> 配合仓库阅读:每个阶段都标注了对应的关键文件,建议边读文档边开仓库对照。
>
> 范围:本仓库当前包含 Spring Boot 后端与 Vue 3 公开站(`ui`)。控制台(`kagami-ui`)与旧公开站(`front`)是前身仓库的阶段产物,代码不在本仓库——Phase 3 / Phase 5 / 阶段 9 保留作为路线图与踩坑记录,其中的路径指向前身仓库。踩坑编号为历史编号,因此有跳号。

---

## 怎么用这份文档

1. **先看全景**(下一节),理解最终形态,再倒推路径;
2. **按阶段推进**,每个阶段坚持"结束时项目是能跑的完整闭环"——不要先铺三个月地基;
3. 每个阶段的**自测题**先自己想/动手验证,再回代码里找答案;
4. **踩坑实录**建议在对应阶段做完后再读第二遍——第一遍读是故事,踩过之后再读是疫苗;
5. 卡住时把问题描述给 AI 助手,但要求它"只给方向不给答案",自己走完最后一步。

---

## 全景:你在造一个什么东西

```
┌──────────────┐        ┌─────────────────────────────────────┐
│ 作者(浏览器)  │        │      Spring Boot 后端 (:8080)        │
│ :5173 前端    ├──────► │  ├─ /api/register /login /me        │
│ 控制台+公开站 │  JWT   │  ├─ /api/posts/**  公开读(匿名+限流)  │
│ (Vue 3)      │        │  ├─ /api/manage/posts/** 管理(JWT)   │
└──────────────┘        │  ├─ /api/**/comments 评论(读匿名/写JWT)│
                        │  └─ ResultWrapperAdvice 统一信封       │
                        └──────────────┬──────────────────────┘
                                       │ MyBatis-Plus / Flyway
                        ┌──────────────▼──────────────────────┐
                        │        PostgreSQL 18 (:5432)         │
                        │   users / posts / tags / post_tags   │
                        │            / comments                │
                        └─────────────────────────────────────┘
```

**选型理由**(面试常问,值得背下来):

| 选择 | 理由 | 被否掉的方案 |
| --- | --- | --- |
| 不引入组件库,原生控件 + CSS token | 目标视觉是 Material You 圆角风格,组件库带"后台脸"且内部样式与 token 对抗;项目页面少,自建控件成本可控 | naive-ui / Vuetify 等组件库(视觉定制受限、包体积大) |
| bigserial 而非雪花 id | id 要经 JSON 进浏览器,必须落在 JS Number 安全整数内 | MyBatis-Plus 默认雪花(踩坑 #1) |
| 无状态 JWT 而非 Session | 前后端分离 + 未来多端,服务端不存会话 | HttpSession(水平扩展要粘滞/共享) |
| 发布是显式端点而非 update 字段 | 误点保存永远不会误发布 | `PUT` 里带 `status` 字段 |
| 原始 Markdown 入库、渲染交给客户端 | 后端不背 Markdown 解析器,换渲染器不动数据 | 后端存渲染后的 HTML |

---

## 路线图:推荐顺序 vs 本仓库实际顺序

推荐顺序:**地基 → 用户系统 → 内容模型 → 控制台(先能写)→ 评论与测试 → 体验收尾**。

前身仓库早期曾先做公开站再补控制台,公开站模块后来整体移除;两种顺序都成立——**关键是每个阶段结束时都能端到端跑通**,而不是追求某个"标准顺序"。

---

## Phase 0:地基(骨架与约定)

**目标**:一个能启动的 Spring Boot,带统一响应信封、全局异常、Flyway 管理的 users 表。

**设计决策**:
- 所有响应包 `R<T>{code, msg, data}` 信封,由 `ResultWrapperAdvice` 自动包裹——Controller 返回裸 DTO。信封让前端错误处理只认一个形状;`code` 用 5 位数字并**按域分段**(1xxxx 用户、2xxxx 内容、9xxxx 系统),前端可按首位分支。
- 时间只用注入的 `Clock`,数据库不写触发器——**单一写入方**,测试可替换时钟。
- 异常只有两种出口:`BusinessException`(业务,带 ErrorCode)和框架异常(校验 400/系统 500),全在 `GlobalExceptionHandler` 收口。

**知识点**:`@RestControllerAdvice` + `ResponseBodyAdvice`;Jakarta Validation(`@Valid`、字段错误如何进 `data` 供前端标红);Flyway 的 `baseline` 与 `validate`;`MetaObjectHandler` 审计字段自动填充。

**踩坑**:无(这一阶段的意义就是让后面的坑有统一的承重墙)。

**自测**:为什么信封包裹要用 ResponseBodyAdvice 而不是每个 Controller 手动 `R.success(...)`?(提示:漏包/嵌套/String 序列化的特殊性。)

---

## Phase 1:用户系统(JWT 与安全)

**目标**:注册/登录/JWT 签发,`/api/me` 需登录,匿名端点声明式管理。

**设计决策**:
- **无状态 JWT(RS256)**:私钥签名、公钥验签,密钥不进仓库。不用 Session——前后端分离 + 未来多端。
- **匿名端点声明式**:`@AnonymousAccess` 注解 + 启动时收集(`AnonymousUrlCollector`),SecurityConfig 里一行 `requestMatchers(anonymousUrlCollector.getAnonymousUrls())`——新增公开接口零配置改动。
- **登录失败统一报"用户名或密码错误"**,防账号枚举;BCrypt(强度 10)存哈希。
- **限流按"路由模式 + 客户端 IP"**:bucket4j,`@RateLimit` 注解声明,拦截器统一执行;IP 取 `X-Forwarded-For` 首段(反向代理场景下取真实客户端地址)。

**知识点**:Spring Security 过滤器链、`OncePerRequestFilter`、SecurityContext 与 principal;JWT 结构(header.payload.signature)、过期与无刷新的设计取舍;bucket4j token bucket 语义(突发 + 匀速补充);Caffeine 本地缓存。

**踩坑**:Spring Security 7 的 `DaoAuthenticationProvider` 配置会产生一条"UserDetailsService 将不被自动使用"的警告——理解它比消掉它更重要:显式声明 provider 就是有意为之。

**自测**:为什么 JWT 不需要服务端存储也能认证?被人偷走 token 能在服务端"踢下线"吗?要支持踢下线需要加什么?

**练习**:给 `/api/me` 加一个修改昵称的接口(PUT),体验"需登录 + 校验 + 信封"全链路。

---

## Phase 2:内容模型(posts/tags)

**目标**:三张表、公开只读 API、作者管理 API、分页信封。

**设计决策**:
- **`content` 存原始 Markdown**,渲染交给客户端——后端永不背 Markdown 解析器。
- **`slug` 可空唯一**:PG 唯一索引允许多个 NULL,不填 slug 的文章用 id 访问;**纯数字 slug 被服务层拒绝**(纯数字是 id 的命名空间)。
- **发布是显式端点**(`/publish`),不是 update 里的一个字段——误点保存永远不会误发布;`published_at` 首次发布时经 `Clock` 写入,幂等(重复 publish 无操作)。
- **分页信封 `PageResponse<T>`**(items/total/page/size)独立于 R 信封;**MyBatis-Plus 分页拦截器必须手动注册**,否则 selectPage "看起来能用"但 total 恒 0。
- 列表组装防 N+1:一页文章 = 1 次分页查询 + 1 次 `IN` 批量取作者 + 1 次 `IN` 批量取标签,共 3 条 SQL。
- **post extras 走增量迁移**:V3 加 `pinned`、字数、阅读时长;置顶列表 `ORDER BY pinned desc, published_at desc`,索引跟着排序走(`(status, pinned desc, published_at desc)`);相关字段在写入时一次算好,不在读取时近似推算。

**知识点**:MyBatis-Plus `LambdaQueryWrapper`(以及它的急切求值陷阱,见踩坑 #3)、自定义 XML + resultMap(带 join 的查询)、`Page` 首参自动分页;3.5.9+ 分页插件拆分到 `mybatis-plus-jsqlparser` 的事实;复合主键连接表(无 BaseMapper,纯 XML);索引与 ORDER BY 的匹配。

**踩坑实录**:
1. **雪花 id 超出 JS 安全整数**(本仓库最大的根因级坑):实体不配 `@TableId` 时 MP 默认 ASSIGN_ID(雪花,约 2^63),超过 JS `Number.MAX_SAFE_INTEGER`(2^53-1),JSON 到浏览器精度丢失,前端拿着错的 id 去发布 → "No such post"。修复:`@TableId(type = IdType.AUTO)` 对齐 bigserial。**教训:凡是会往返于浏览器的数值 id,要么保证 < 2^53,要么序列化成字符串。**
2. **wrapper 条件参数急切求值**:`.like(condition, col, keyword.trim())` 的 condition 只控制 SQL 拼接,**参数永远被求值**——keyword 为 null 直接 NPE。修复:先把 keyword 归一化再传入。
3. **分页拦截器没注册**:症状是"分页查询单页能出数据、total=0"——MP 的坑在于它不报错,只是静默退化。
4. **LIKE 通配符**:用户搜 `100%` 会命中 `100` 加任意后缀;入库前转义 `%`、`_`(以及 `\`)。
14. **同一 SELECT 里两个独立 random()**(造数时):`status` 与 `published_at` 各自随机,产生"已发布但 published_at 为 null"的脏行。**相关性必须在数据生成时就保证。**
15. **全表字段回填**(迁移时):`greatest(1, ceil(char_length/400))` 这类近似回填先让 UI 不出 0,精确值等下次写入覆盖——渐进迁移的常用手法。

**自测**:为什么列表接口不查 `content` 列?如果文章表有 100 万行,不投影会发生什么?`@TableId(type = IdType.AUTO)` 与数据库的 bigserial 是什么关系?

**练习**:给 posts 加一个 `category`(单选)列并出现在列表/详情/编辑器——走一遍 Flyway 增量 + 实体 + DTO + 前端展示的完整链路。

---

## Phase 3:控制台(编辑器与管理)

> 本阶段的控制台前端不在本仓库(见顶部"范围")——保留作为设计决策与踩坑的路线图。

**目标**:登录后的文章列表 + 分栏 Markdown 编辑器,消费同一套后端 API。

**设计决策**:
- 控件全部原生(`<table>`/`<input>`/`<select>`/`<button>`) + 全局 token 样式类,零组件库;危险操作确认与反馈统一走 `useFeedback()` 的 Promise 化 `confirm()` 与 toast,宿主组件 `FeedbackHost.vue` 挂一次、无需 provider context。
- 编辑器 = 左原生 `<textarea>` + 右实时预览(300ms 防抖,markdown-it + shiki);**脏检查**用"表单快照 JSON 对比"而非散落的 dirty 标志,`onBeforeRouteLeave` 用同一个 Promise 化 `confirm()` 拦截。
- 新建保存后 `router.replace` 到编辑路由——URL 与状态始终一致,刷新不丢。

**知识点**:原生表单 `v-model` 与事件签名;原生 `<table>` 表头 sticky 与空态;vue-tsc 对模板事件签名的检查;shiki 单例 + 未知语言护栏。

**踩坑实录**:
9. **路由守卫里的确认必须是 Promise**:`onBeforeRouteLeave` 要等用户决定,`confirm()` 返回 `Promise<boolean>`;取消(背景点击/Escape/负面按钮)一律 resolve `false`,否则守卫永远不放行。
10. **重复导入**:同一模块的 `import { ... }` 与 `import type { ... }` 触发 no-duplicate-imports——用行内 `type` 修饰符合并。

**自测**:为什么编辑器的预览要防抖?脏检查为什么用快照对比而不是监听每个字段?

**练习**:给编辑器加"字数与预计阅读时长"实时显示。

---

## Phase 4:评论模型与 API(第一次写测试)

**目标**:两层盖楼评论(顶层 + 扁平回复),公开只读 + 登录发表 + 文章作者审核(隐藏/恢复/删除),并让项目第一次拥有测试基建(Testcontainers + MockMvc)。

**设计决策**:
- **两层盖楼而非无限嵌套**:`parent_id` 要么 NULL(顶层)要么指向同文章的顶层评论 —— 无递归查询、无缩进树,校验一条"回复只能指向顶层"就完事;自引用 FK `on delete cascade` 让删除顶层评论时回复一起走。
- **无待审状态**:评论者仅限登录用户,滥用风险低,审核就是文章作者的隐藏/恢复(`status` 0/1),不需要预审核状态机。
- **测试用 Testcontainers 而非 H2**:评论 SQL 全是普通 PG 语法,H2 的 PG 兼容模式会有惊喜;Testcontainers 起真实 PG 18.4 容器,Flyway 全量迁移,和线上一致。

**知识点**:`@ServiceConnection` 让 Spring Boot 自动把容器连接参数喂给 DataSource;`@AutoConfigureMockMvc` 在 Boot 4 拆到了 `spring-boot-starter-webmvc-test`(独立 starter);测试内直接用 `JwtTokenProvider` 签发 token(不走登录接口,隔离限流);`@Transactional` 测试回滚实现用例隔离;MockMvc 断言 R 信封(`$.code`/`$.data[0].replies.length()`)。

**踩坑实录**:
21. **测试隔离**:没用 `@Transactional` 时,`selectList(null)` 统计到了其他测试留下的数据 —— 断言必须按 postId 过滤,或者干脆加事务回滚(选了后者)。
22. **Boot 4 测试注解分包**:`@AutoConfigureMockMvc` 从 `org.springframework.boot.test.autoconfigure.web.servlet` 搬到了 `org.springframework.boot.webmvc.test.autoconfigure`,且需要独立的 `spring-boot-starter-webmvc-test`;Jackson 3 的包名是 `tools.jackson` 而非 `com.fasterxml`。
23. **Testcontainers 版本不被 Boot BOM 管理**(Boot 4.1),需要显式版本号。

**自测**:两层盖楼下"回复只能指向顶层"这条规则应该校验哪些条件(存在/同文章/顶层)?删除顶层评论时回复怎么办,由数据库还是应用负责?为什么评论 API 的测试要用真实 PG 而不是 mock Mapper?

**练习**:给评论加"编辑"(作者本人,5 分钟窗口);给 `GET /api/manage/comments` 加分页。

---

## Phase 5:控制台评论审核 UI

> 同 Phase 3:控制台前端不在本仓库,保留作为路线图与踩坑记录。

**目标**:控制台出现评论审核页(隐藏/恢复/删除),让读者评论区的管理闭环落在作者手上。

**设计决策**:
- **控制台直连后端**:用登录态拿到的 JWT 调 `/api/manage/comments`,与公开只读评论 API(`/api/posts/{slugOrId}/comments`)是两条独立通道,各走各的最清晰。
- **后端返回组装好的树**(顶层 + `replies[]`),前端只做展示,不再做分组合并;隐藏顶层评论时其回复在组装层一并下架(不级联改状态),恢复后原样出现——不需要"记住哪些回复原本可见"(踩坑 #26)。

**知识点**:R 信封在前端取数层的统一解包;按 postId 过滤的管理列表;危险操作确认(Popconfirm)与操作反馈(useMessage)。

**踩坑实录**:
24. **取数忘记解 R 信封**:拿到的是 `{code,msg,data}` 整体 —— `threads.length` 为 undefined,列表永远为空且无报错。修复:在取数层统一 transform 解包。教训:**同一个数据形状,在不同取数通道里解包位置必须一致**。
26. **隐藏评论的回复处理**:顶层被隐藏时其回复在组装层一并下架,恢复后原样出现 —— 不需要"记住哪些回复原本可见"。

**自测**:为什么管理接口要按 postId 过滤而不是拉全量再前端筛?隐藏顶层评论时为什么不在数据库级联改回复状态?

**练习**:给审核列表加分页;给回复加"查看对话"上下文跳转。

---

## 阶段 9:公开站前端(front)

> 本阶段记录的是前身仓库中的旧公开站(`front`);当前仓库的公开站重建为 `ui/`(见 `ui/README.md`),设计改为单色相 OKLCH token 体系。本节保留作为历史记录。

**目标**:按设计图还原一个可直接运行的个人小屋公开站——首页、文章列表、文章详情、归档、标签、小屋(关于 & 收藏),含移动端适配与明暗主题切换。

**设计决策**:
- 数据契约集中在 `front/src/types`,`data/mock.ts` 的 `Post` 形状对齐后端公开 DTO;日后接真实 API 只换数据层,组件不动。
- 图片全部走 `PlaceholderImage`:`src` 为空渲染占位,后期填入路径即自动替换,页面不需要知道自己展示的是不是真图。
- 设计 token 集中在 `front/src/styles/tokens.css`(镜紫/樱花粉/奶油白/晴空蓝/深紫),组件只消费变量,不硬编码颜色。
- 路由统一挂在 `SiteLayout` 下;搜索与标签筛选的状态与 URL query 双向同步,刷新不丢筛选条件。

**知识点**:Vue Router 嵌套路由与 `scrollBehavior`(锚点偏移)、markdown-it 的 `html: false` 安全渲染、CSS `aspect-ratio` + `clamp()` 做比例还原、`:root[data-theme]` 切换与 localStorage 持久化、TypeScript 6 的路径别名写法。

**踩坑实录**:
27. **npm 全局 `omit=dev`**:`npm install` 只装生产依赖,`vue-tsc` 不在 PATH,构建脚本报 "'vue-tsc' is not recognized"。修复:`npm install --include=dev`。教训:**构建工具缺失先查 npm 配置,不要急着改 scripts**。
28. **TypeScript 6 弃用 `baseUrl`**:`paths` 别名需要写成 `"@/*": ["./src/*"]`(带 `./`),否则先报 TS5101 再报 TS5090。教训:**TS 6 起路径别名不再需要 `baseUrl`,映射值必须是相对路径**。

**自测**:为什么图片占位要做成组件,而不是在每个页面写一个 `<div>`?如果要接真实 API,需要改哪些文件?

**练习**:把首页文章列表换成 `/api/posts` 的真实数据(保留加载态与错误态);给收藏项加独立详情页。

---

## 踩坑总表(按域速查)

| 域 | 坑 | 一句话教训 | 仓库位置 |
| --- | --- | --- | --- |
| 后端 | 雪花 id 超 JS 安全整数 | 会往返浏览器的 id 必须 < 2^53 或序列化为字符串 | `Post.java` 的 `@TableId` |
| 后端 | 超 2^53 的数值 id 经 JS JSON 序列化被静默舍入(JWT payload 的 user_id 被舍入,按 id 查询的接口 401) | 同上,且服务端工具链(测试脚本/Node)同样适用;按 username 查询的接口不报错,按 id 查询的才暴露 | `JwtTokenProvider` 签发与 JWT payload |
| 后端 | MP 分页拦截器不注册则静默退化 | 全局基础设施要在 Phase 0 一次配好 | `data/config/MybatisPlusConfig.java` |
| 后端 | wrapper 条件参数急切求值 | 先归一化参数,再传给 wrapper | `PostService.pageMine` |
| 后端 | MetaObjectHandler 不覆盖 wrapper 更新 | wrapper-only update 手动 set updatedAt | `PostService.update` |
| 后端 | mybatis-plus-jsqlparser 3.5.9+ 拆分 | 分页依赖要显式声明 | `pom.xml` |
| 后端 | LIKE 通配符未转义 | 用户输入进 LIKE 前转义 `%`/`_` | `PostService.escapeLike` |
| 后端 | 限流桶按代理 IP 共享 | 反向代理必须转发真实客户端 IP | `RateLimitInterceptor` |
| 数据库 | 无关联 LATERAL 只求值一次 | 让子查询引用外层列 | `docs/mock_data.sql` |
| 数据库 | 同一 SELECT 双 random() 产生不一致行 | 相关字段从同一随机源推导 | `docs/mock_data.sql` |
| 数据库 | 修改已应用的 Flyway 迁移文件(哪怕只改一行注释)导致 checksum mismatch,应用启动失败 | 已应用的迁移不可变——注释、格式、任何字节都不例外;文档性修正另开迁移或放弃 | `V2__content_init.sql` |
| 控制台 | 路由守卫的确认没返回 Promise,守卫永远卡住 | `confirm()` 返回 `Promise<boolean>`,取消路径也要 resolve | `useFeedback.ts` / `PostEditorView.vue` |
| 控制台 | 取数忘解 R 信封,列表恒空且无报错 | 取数层统一 transform 解包 | `PostCommentsView.vue` / 请求封装 |
| 公开站 | npm 全局 `omit=dev` 使 devDependencies 不安装,`vue-tsc` 找不到 | `npm install --include=dev` 显式补齐;构建失败先查依赖树 | `front/package.json` |
| 公开站 | TypeScript 6 弃用 `baseUrl`,`paths` 映射必须相对 | 用 `"@/*": ["./src/*"]`,不要再写 `baseUrl` | `front/tsconfig.app.json` |
| 公开站 | 双击无文本区域时浏览器选词算法命中 DOM 中最近的文本节点,随机高亮无关文本 | 双击切换是全站手势,就在 `body` 设 `user-select: none` 作默认,再用 `.selectable` 白名单恢复正文;逐个容器打补丁会漏新增区域 | `ui/src/style.css` 的 `body` / `.selectable` |
| 公开站 | `100vw` 含滚动条宽度,而 fixed 元素的定位基准(布局视口)不含,用它算停靠坐标会钻到视口外 | 停靠位置写在 `inset: 0` 的 fixed 容器里用 `left: calc(100% - …)`,`100%` 按真实布局宽度解析 | `ui/src/components/VinylPlayer.vue` |
| 公开站 | CSS 形变两端写法不一致(如 `left: 50%` → `auto`)时 `auto` 不参与插值,位置直接瞬移 | 形变两端用同一组可插值 longhand(`calc()` 长度),旋转等 transform 动画放子元素,不与形变争同一个 transform | `ui/src/components/VinylPlayer.vue` |
| 公开站 | `pointer-events: none` 只影响命中测试、不影响事件冒泡:穿透层上的子控件双击仍会冒泡触发祖先的全局 dblclick 手势 | 全局 dblclick 监听所在的祖先,要靠每个可交互子控件 `@dblclick.stop` 显式拦停 | `ui/src/components/VinylPlayer.vue` |
| 公开站 | 展开态与迷你态共用同一个唱片元素,展开态 mouseenter 埋下的 hover 状态会在收起瞬间让悬浮条凭空弹出(指针明明不在其上) | "指针当前是否在上"这类瞬态只在对应形态下跟踪:监听入口按形态门控,形态切换时重置;事件驱动的状态会过期,CSS `:hover` 那种按帧求值才是真状态 | `ui/src/components/VinylPlayer.vue` |
| 公开站 | 形变飞行的元素扫过静止指针时,Chrome 按"元素移入光标下"补发 mouseenter,悬浮条在飞行途中误弹出 | 形变期间抑制 hover 监听,落定后用 `matches(':hover')` 复核一次:既拦住扫过的误触发,又不丢"指针恰好停在落点"的真悬浮 | `ui/src/components/VinylPlayer.vue` |
| 公开站 | 全屏舞台背后的页面仍可滚动,滚动条浮在覆盖层上;滚动锁加在 body 上经传播才生效不可靠,且 `scrollbar-gutter` 预留的 gutter 条在覆盖层外露出异色边 | 滚动锁挂根元素:`html.player-expanded { overflow: hidden }` 直接管视口,不依赖 body→视口传播;gutter 条画的是画布背景(html/body 背景传播),展开态把 html 背景设为 `--color-intro-bg` 与覆盖层同色即无接缝;`scrollbar-gutter: stable` 保持布局宽度恒定,解锁零回流 | `ui/src/stores/player.ts` / `ui/src/style.css` |
| 工程化 | @Valid 的错误映射进 data | 前端字段标红依赖这个形状 | `GlobalExceptionHandler` |

## 知识点地图(按域,附仓库锚点)

**后端(Spring Boot / Security / MyBatis-Plus)**:R 信封与 ResponseBodyAdvice(`common/web/ResultWrapperAdvice`)、全局异常与 ErrorCode 分域(`common/exception/`)、审计自动填充与 Clock(`data/handler/`、`common/time/`)、过滤器链与 JWT(`security/`)、声明式匿名端点(`security/config/AnonymousUrlCollector`)、bucket4j 限流(`security/ratelimit/`)、MP 三件套(实体/Mapper/XML/Wrapper)与分页(`post/`)。

**数据库(PostgreSQL / Flyway)**:bigserial 与身份策略、timestamptz + 应用侧写入、唯一索引与 NULL(`V2`)、增量迁移与回填(`V3`)、索引跟随 ORDER BY(`V3`)、LATERAL/random 造数陷阱(`docs/mock_data.sql`)。

**控制台(Vue / 原生控件;前身仓库,代码不在本仓库)**:CSS token 与全局控件类(`kagami-ui/src/styles/index.css`)、Promise 化确认与 toast(`useFeedback.ts` + `FeedbackHost.vue`)、vue-tsc 的模板类型检查、R 信封在客户端的统一解包、markdown-it + shiki 预览(`kagami-ui/src/utils/markdown.ts`)。

**公开站·前身(front;代码不在本仓库)**:集中式 token 层(`front/src/styles/tokens.css`)、统一图片占位(`PlaceholderImage.vue`)、类型化 mock 与派生数据(`front/src/data/mock.ts`)、嵌套路由与锚点滚动(`front/src/router/index.ts`)、markdown-it 安全渲染(`PostDetailView.vue`)。

**公开站·当前(ui)**:单色相 OKLCH token 层,一个 `--hue` 驱动明暗两套(`ui/src/styles/tokens.css`)、常驻唱片的形态形变与停靠(`ui/src/components/VinylPlayer.vue`)、本地 markdown 数据层与可替换数据源(`ui/src/data/`)、Pinia 状态(`ui/src/stores/`)。

**工程化**:统一信封的跨端契约、README 体系、语义化提交与阶段化交付、文档即路线图(本文)。

## 延伸路线(按性价比排序)

1. **测试**:评论 API 已有 Testcontainers 集成测试;下一步是 PostService/TagService 单元测试(Mockito)+ 公开文章 API 的 MockMvc 集成测试。
2. **CI**:GitHub Actions 跑 `mvnw verify` + `ui` 的 `npm run build`。
3. **部署**:compose.yaml 补 `kagami-api` 服务;公开站已在 `ui/`,下一步接入真实 API,再规划其渲染层与站点 URL 配置。
4. **缓存演进**:限流的 Redis 后端(`BucketProvider` 接口已预留)、列表接口加 Redis 缓存。
5. **功能线**:PG 全文检索、浏览量/点赞、友链页、公开站接入真实数据。

## 学习方法建议

- **读代码的推荐顺序**:`R.java` → `GlobalExceptionHandler` → `SecurityConfig` → `PostService` → `PostMapper.xml` → `CommentService` → `ui/src/data/sources/local.ts` → `ui/src/stores/player.ts` → `ui/src/components/VinylPlayer.vue`。这条线覆盖了全部核心概念,其余都是变奏。
- **改前先跑通、改后必验证**:本项目的每个坑都是在"改完立刻实测"里暴露的。
- **把每个坑写成一句话教训**(就像踩坑总表那样),比收藏十篇博客有用。
- 随时可以把本文的某道自测题丢给 AI 助手对答案、要求出变式题、或要求"只提示不给答案"地陪你走某个踩坑点——**让 AI 当陪练,不当替身**。
