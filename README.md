# Material Studio

一个可独立运行的素材制作网站。用户可以浏览模板、上传素材、提交异步制作任务，并在任务处理期间继续创建其他作品；任务结束后可以从个人记录中查看结果。

制作过程不调用真实 AI 服务。Java 后台按「素材检查 → 模拟生成 → 结果整理」顺序执行，并通过 Java2D 将上传图片合成为可预览的 PNG。前端以 2 秒轮询展示服务端状态，页面关闭或查询失败不会中断后台任务。

## 技术版本

| 模块 | 版本 |
| --- | --- |
| Next.js | 16.4.3，App Router |
| React | 19.3.1 |
| TypeScript | 5.9.3 |
| 前端依赖管理 | pnpm 11.25.0 |
| Java | 17 |
| Spring Boot | 4.1.0 |
| MyBatis | 4.1.0 |
| Flyway | 12.4.0 |
| MySQL | 8.4（Compose） |
| 测试 | JUnit 6、Testcontainers 2.0.5、Vitest 5.0.5 |

## 项目结构

```text
.
├── frontend/                 Next.js 页面、客户端状态和前端测试
├── backend/                  Spring Boot、MyBatis、异步 Worker 和后端测试
├── .github/workflows/ci.yml  GitHub Actions
├── compose.yaml              MySQL、Java、Next.js 编排
└── .env.example              非敏感配置示例
```

数据库迁移位于 `backend/src/main/resources/db/migration`。模板封面位于 `backend/src/main/resources/static/media/templates`，没有依赖付费图库或公司内部资源。后端运行镜像安装 Noto CJK 字体，确保生成结果中的中文文案正常显示。

## 验收版本

源码验收 commit：`12877b84e10da7db8ee11091120f3f18a36cca08`。该提交包含本次运行镜像、响应式布局、弹窗可访问性、会话配置和测试修复；其后的文档提交仅记录本信息。

## 一键启动

前置条件：Docker Desktop 或兼容的 Docker Engine，以及 Docker Compose v2。

```powershell
docker compose up --build
```

打开 <http://localhost:3000>。

服务说明：

- 前端：`http://localhost:3000`
- Java API：`http://localhost:8080/api`
- 健康检查：`http://localhost:8080/actuator/health`
- MySQL：宿主机 `localhost:3307`，容器内 `db:3306`

宿主机使用 3307 是为了避免覆盖本机现有的 3306 MySQL。MySQL 数据保存在 `mysql_data` 卷，上传与结果文件保存在 `app_data` 卷。

停止服务：

```powershell
docker compose down
```

如需连同本地数据库和媒体文件一起清除，可显式执行 `docker compose down -v`。该命令会删除卷，不属于普通重启步骤。

## 配置

Compose 提供可直接演示的开发默认值。生产或共享环境应复制 `.env.example` 为 `.env`，并修改所有密码和 `APP_SESSION_SECRET`。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_DATABASE` | `material_studio` | 数据库名 |
| `MYSQL_USER` | `material` | 数据库用户 |
| `MYSQL_PASSWORD` | `material_dev` | 数据库密码 |
| `MYSQL_ROOT_PASSWORD` | `root_dev` | MySQL root 密码 |
| `DB_IMAGE` | `mysql:8.4` | 可替换为当前缓存且兼容的 MySQL 镜像 |
| `SPRING_DATASOURCE_URL` | Compose 自动设置 | JDBC URL |
| `APP_SESSION_SECRET` | 本地开发值 | 签名 Cookie 的 HMAC 密钥，至少 24 字符 |
| `APP_SESSION_SECURE` | `false` | HTTPS 部署时设为 `true` |
| `APP_STORAGE_ROOT` | `/app/data` | 上传和结果目录 |
| `BACKEND_ORIGIN` | `http://backend:8080` | Next.js API 重写目标 |

## 本地开发

可仅用 Compose 启动数据库：

```powershell
docker compose up -d db
```

启动 Java：

```powershell
cd backend
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3307/material_studio?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
mvn spring-boot:run
```

启动前端：

```powershell
cd frontend
pnpm install --frozen-lockfile
$env:BACKEND_ORIGIN="http://localhost:8080"
pnpm dev
```

Flyway 会在后端首次启动时自动创建表并插入两个演示用户、6 个模板及分类。无需手工执行 SQL。

前端 `/api/*` 请求由 Next.js 同源重写到 Java；Java 始终负责模板、素材、任务、状态、历史和个人记录业务。

## 用户与个人记录

网站提供两个演示身份：

- 林小满：`xiaoman@example.test`
- 陈屿：`chenyu@example.test`

首次访问选择身份，顶栏头像菜单可随时切换或退出。服务端将用户 ID 与过期时间放入 HMAC 签名 Cookie，Cookie 为 HttpOnly、SameSite=Lax；客户端不能直接修改用户 ID。每个素材、任务和历史查询都绑定当前用户。

这是面向演示的账号切换，不是生产认证系统。没有密码、注册、角色或账号管理；如需生产化，应替换为正式身份服务，并增加 CSRF、防暴力登录和审计策略。

## 素材规则

- 每个模板定义所需图片数量，示例模板需要 1 至 3 张。
- 支持 JPEG 和 PNG；服务端根据文件签名识别真实格式，不只信任 MIME。
- Spring multipart 全局上限为 100MB；各模板另有 5MB、8MB 或 10MB 限制。
- 服务端验证素材归属、文件大小、可解码性和像素上限。
- 上传成功后素材持久化到后端目录，而不是浏览器临时地址。
- 前端逐图片位显示本地预览、上传中、失败、替换和移除状态。

## 接口

所有 `/api` 业务接口除 `/api/auth/*` 和模板封面外都需要有效的演示会话 Cookie。错误统一返回：

```json
{
  "code": "INVALID_ASSET_COUNT",
  "message": "该模板需要 2 张图片",
  "fieldErrors": null
}
```

### 身份

| 方法 | 路径 | 请求 / 响应 |
| --- | --- | --- |
| `GET` | `/api/auth/me` | 返回 `{currentUser, users}`；未登录时 `currentUser=null` |
| `POST` | `/api/auth/demo` | `{userId}`；设置 Cookie 并返回身份 |
| `POST` | `/api/auth/logout` | 清除 Cookie，返回 204 |

### 模板

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/templates?category=` | 返回 `{templates, categories}` |
| `GET` | `/api/templates/{id}` | 返回模板详情与上传要求 |

模板主要字段：

```json
{
  "id": "travel-postcard",
  "title": "旅行明信片",
  "category": "创意图片",
  "coverUrl": "/api/media/templates/travel.svg",
  "description": "把旅途里的两张照片排成一张有呼吸感的明信片",
  "requiredImageCount": 2,
  "acceptedMimeTypes": ["image/jpeg", "image/png"],
  "maxFileSizeBytes": 5242880
}
```

### 素材

| 方法 | 路径 | 请求 / 响应 |
| --- | --- | --- |
| `POST` | `/api/assets` | multipart 字段 `file`；返回素材 ID、文件名、类型、大小及宽高 |
| `DELETE` | `/api/assets/{id}` | 删除尚未用于任务的素材；已使用素材返回 409 |

### 任务

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/tasks` | `{templateId, assetIds}`；完成校验和记录创建后返回 202 |
| `GET` | `/api/tasks` | 当前用户全部记录，按创建时间倒序 |
| `GET` | `/api/tasks/{id}` | 当前用户单任务状态 |
| `GET` | `/api/tasks/{id}/result` | 校验所有权后以内联 PNG 返回结果 |

任务响应：

```json
{
  "id": "f0d1...",
  "templateId": "travel-postcard",
  "templateTitle": "旅行明信片",
  "coverUrl": "/api/media/templates/travel.svg",
  "assetCount": 2,
  "status": "PROCESSING",
  "stage": "GENERATION",
  "failedStage": null,
  "failureReason": null,
  "resultUrl": null,
  "createdAt": "2026-10-31T23:00:00+08:00",
  "updatedAt": "2026-10-31T23:00:02+08:00"
}
```

## 状态机与后台执行

任务状态：

- `WAITING`：已受理并进入本地线程池队列。
- `PROCESSING`：Worker 已领取任务。
- `SUCCEEDED`：结果已保存。
- `FAILED`：步骤失败或强制模拟失败。

执行阶段：

- `VALIDATION`：复核素材数量、归属、文件和可解码性。
- `GENERATION`：按模板生成 PNG 拼贴。
- `ASSEMBLY`：整理最终文件并写入成功状态。
- `COMPLETED`：成功终态。

每个阶段开始前更新数据库并可配置独立耗时。阶段失败后停止后续步骤，`failedStage` 和 `failureReason` 同时持久化。查询接口只读取数据库，不推进任务。

后台使用固定 4 线程、容量 100 的队列。多个任务可独立排队或并行，页面关闭不影响已经进入 Java Worker 的任务。

同一用户的记录通过用户 ID 隔离；状态更新使用 `WHERE status` 条件，终态不会被后续查询改回处理中。每个任务的模拟配置在提交时写入任务记录，因此修改全局模拟设置不会影响已创建任务。

## 演示成功、失败和多任务

### 正常成功

1. 选择一个模板并上传规定数量的图片。
2. 点击“开始制作”。
3. 任务卡片依次显示检查、生成、整理阶段。
4. 成功后可预览服务端生成的 PNG 结果。

### 指定阶段失败

1. 点击顶栏的烧瓶按钮打开隐藏的“后台模拟设置”。
2. 选择 `素材检查`、`模拟生成` 或 `结果整理` 作为强制失败阶段。
3. 保存后创建新任务。
4. 任务会运行到指定阶段，记录失败原因并停止后续步骤。

也可通过 API 设置：

```powershell
$body = '{"stepDelayMs":2000,"failStage":"GENERATION"}'
Invoke-RestMethod -Method Put -Uri http://localhost:3000/api/dev/simulation `
  -ContentType 'application/json' -Body $body -WebSession $session
```

读取配置：

```powershell
Invoke-RestMethod -Uri http://localhost:3000/api/dev/simulation -WebSession $session
```

`stepDelayMs` 范围为 0 至 60000 毫秒，默认 1200。设回 `failStage: null` 即关闭模拟失败。

### 多任务与刷新恢复

调整耗时后连续提交 2 至 4 个作品。任务拥有独立 ID、阶段、错误和结果，一个任务失败不会改变其他任务。

刷新页面后，前端会从 `/api/tasks` 恢复记录。只要当前用户仍有 `WAITING` 或 `PROCESSING` 任务，就每 2 秒查询一次；全部进入终态后停止轮询。查询网络错误只显示“查询暂时不可用”，保留最后一次任务快照并允许手动恢复，不会把后台任务标记为失败。

切换演示用户时会取消并清理上一用户的业务缓存，旧请求不会覆盖新用户记录。

## 测试与 CI

前端：

```powershell
cd frontend
pnpm run lint
pnpm run typecheck
pnpm test
pnpm run build
```

后端：

```powershell
cd backend
mvn -B verify
```

后端测试需要 Docker，使用 Testcontainers 启动隔离 MySQL。集成测试覆盖：

- 成功任务执行及终态查询幂等；
- `VALIDATION` 和 `GENERATION` 强制失败中断；
- 多任务状态互不串扰；
- 素材和任务跨用户隔离；
- 非图片文件及错误素材数量校验。

前端测试覆盖：

- 任一活动任务存在时轮询、全部终态时停止；
- 多任务独立状态和失败展示；
- 查询异常保留已获取任务，且不误报任务失败；
- 阶段索引和终态判断。

当前本地验收结果为前端 5 项测试通过、后端 8 项测试通过，前端生产构建与后端构建均成功。

Testcontainers 测试镜像使用本机可缓存的 `mysql:5.7.36`，用于验证迁移兼容性；正式 Compose 默认使用 MySQL 8.4。容器冒烟验收也通过 `DB_IMAGE=mysql:5.7.36` 完成；当前环境的网络未能拉取完 MySQL 8.4 镜像，因此 8.4 运行镜像属于配置完成但未在本机实际启动的部分。

GitHub Actions 在 push 和 pull request 时执行前端 lint、类型检查、测试、构建，以及后端测试和 Maven 构建。

## 已完成与已知限制

已完成：

- 模板分类、服务端数据、本地封面和详情加载；
- 多图片选择、预览、替换、移除、上传失败反馈；
- 前后端素材校验、用户隔离和签名会话；
- 异步三阶段任务、可调耗时、指定阶段失败、多任务并行；
- 个人历史、刷新恢复、轮询生命周期和查询异常区分；
- 成功结果生成与预览；
- Compose、Dockerfile、Flyway、CI、前后端关键测试和响应式界面。

未实现：

- 真实 AI、视频处理、支付、积分或会员；
- 完整登录注册、账号管理后台、第三方身份；
- 任务重试、删除、取消或下载；
- 消息队列、Redis、SSE 或 WebSocket；
- 服务重启后自动恢复执行。

服务重启限制：已经由客户端提交并持久化的任务记录仍在数据库；本版本没有启动时重新派发 `WAITING` 或中断中任务的机制。重启后这些记录可能需要人工处理。要求明确不包含自动恢复，这里如实保留为限制。

演示身份、客户端轮询和本地文件卷适合本题的完整业务演示，但不是高并发生产方案。生产化时应使用正式认证、对象存储、消息队列、幂等消费、任务恢复监控和集中日志。

## 部署与回滚

部署：

1. 准备 `APP_SESSION_SECRET`、数据库密码和持久卷。
2. 拉取源码验收 commit `12877b84e10da7db8ee11091120f3f18a36cca08`。
3. 执行 `docker compose up --build -d`。
4. 确认 `/actuator/health`、`http://localhost:3000` 和 MySQL 健康状态。

回滚：

1. 切回上一个通过测试的 commit。
2. 执行 `docker compose up --build -d`。
3. 保留 `mysql_data` 和 `app_data` 卷；如迁移不可向后兼容，应先按备份方案恢复数据库。

不要使用删除卷作为普通回滚步骤。

## AI 工作流与实际投入

本题使用 Codex 辅助完成。工作流为：

1. 读取笔试题全部 R-01 至 R-14 和 3 张相关 UI 参考图，将需求拆成后端异步任务、身份隔离、素材交互、前端轮询、工程交付五部分。
2. 明确 Next.js、Spring Boot、MyBatis、MySQL、Compose 和测试边界后生成初始实现。
3. 实际运行 lint、TypeScript、Vitest、Maven、Flyway 和 MySQL 容器测试，根据错误修正框架差异与业务缺陷。
4. 逐项核对验收场景，更新 README 的真实完成度和限制。
5. 根据实际运行反馈修复容器中文字体、小屏导航遮挡、结果弹窗焦点和会话安全配置，并复跑完整检查。

一个具体例子：MySQL 集成测试发现了“指定 `GENERATION` 阶段失败却在 `VALIDATION` 阶段提前触发”的逻辑问题。修复方式是让强制失败判断同时匹配当前阶段，而不是只要配置了失败就立即执行；随后复跑完整测试集确认成功路径、两种失败路径和多任务行为均通过。

实际投入约 2.5 小时，包含需求理解、代码实现、依赖下载、测试修正、交付整理和验收复测，不包含笔试题平台上的等待时间。

## 外部代码与素材

- 使用 Next.js、React、Spring Boot、MyBatis、Flyway、MySQL、TanStack Query、Lucide 图标及测试库，许可证和版本由锁文件及 Maven 依赖管理。
- 6 个模板封面为本项目内置的原生 SVG，没有使用品牌截图、付费图库或公司内部素材。
- 页面视觉参考了题目提供的深色布局信息层级，但文案、品牌、组件结构和素材均为独立实现。
