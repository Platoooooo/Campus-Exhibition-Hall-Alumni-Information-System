# 校园展览馆校友资料系统

校园展览馆校友资料收集 → 多级审核 → 统一入库 → 大屏展示一体化平台。

## 技术栈

| 端 | 技术 | 端口 |
|---|---|---|
| 后端 | Spring Boot 3.x · JDK 17 · MyBatis-Plus · Spring Security(JWT) · MySQL 8.0 · Redis 7 | 8080 |
| 管理后台 | Vue3 · Vite · Pinia · Vue Router · Element Plus · ECharts · Axios | 5173 |
| 大屏展示端 | Vue3 · Vite · Pinia · Vue Router · Axios（轻量，无 Element Plus） | 5174 |
| 人脸识别服务 | Python · FastAPI · ArcSoft 虹软 SDK (ctypes) · Windows x64 | 8000 |

## 目录结构

```
/
├── server/              Spring Boot 后端
│   ├── src/main/java/com/campus/exhibition/
│   │   ├── controller/   REST 接口（Auth/Dashboard/Alumni/Archive/Audit/Operation/Screen/Face/SysUser/Log/...）
│   │   ├── service/      业务逻辑接口 + impl
│   │   ├── mapper/       MyBatis-Plus Mapper
│   │   ├── entity/       数据库实体
│   │   ├── dto/          请求体 / 查询条件
│   │   ├── vo/           返回视图
│   │   ├── common/       统一返回体(R)、异常、AOP日志、数据权限
│   │   ├── config/       Spring 配置（CORS/MyBatis/Security/Cache）
│   │   ├── security/     JWT 过滤器、登录用户、UserDetailsService
│   │   └── enums/        业务枚举（角色/状态/媒体类型）
│   └── pom.xml
├── admin-web/           管理后台（Vue3 + Element Plus）
│   ├── src/
│   │   ├── api/          Axios 请求封装（auth/alumni/archive/audit/dashboard/operation/log/user/...）
│   │   ├── views/        页面（login/dashboard/alumni/archive/audit/operation/system）
│   │   ├── layout/       主框架布局
│   │   ├── router/       路由 + 守卫
│   │   ├── stores/       Pinia 状态（user）
│   │   ├── styles/       全局 CSS + 字体
│   │   └── components/   公共组件
│   └── vite.config.js
├── screen-web/          大屏展示端（Vue3 轻量）
│   ├── src/
│   │   ├── api/          screen/face 请求封装
│   │   ├── views/        页面（carousel/wall/detail/search）
│   │   ├── components/   ParticleBg / FaceOverlay
│   │   ├── composables/  useScale（1920 自适应缩放）
│   │   ├── router/       路由
│   │   └── stores/       Pinia 状态（screen）
│   └── vite.config.js
├── face-service/         人脸识别服务（Python FastAPI）
├── docs/                 设计文档
│   ├── design/           需求/功能/页面/数据库/接口设计
│   └── prompt/           施工 prompt 清单
└── README.md
```

## 默认账号密码

| 服务 | 用户名 | 密码 | 说明 |
|---|---|---|---|
| 管理后台 | `admin` | `admin123` | 超级管理员（角色 admin） |
| MySQL | `root` | `root` | 数据库 root 账户 |
| Redis | — | `redis123` | Redis 访问密码 |
| MinIO | `minioadmin` | `minio123456` | 对象存储（可选） |

> ⚠️ **部署到生产环境前请务必修改所有默认密码。**

## 快速启动

### 前置条件

- JDK 17+
- Maven 3.8+
- Node.js 18+
- Python 3.10+（仅 face-service 需要）
- Docker Desktop（推荐，或手动安装 MySQL 8.0 + Redis 7）

### 方式一：Docker 启动（推荐）

```bash
# 1. 启动 MySQL + Redis(+ MinIO 可选)
cd docker
cp .env.example .env   # 首次需要，按需修改密码
docker compose up -d

# 等待健康检查通过（约 15s），MySQL 自动执行建表 + 初始化数据
docker compose ps   # 确认服务都是 healthy

# 2. 启动后端
cd ../server
mvn spring-boot:run

# 3. 启动管理后台（新终端）
cd ../admin-web
npm install && npm run dev

# 4. 启动大屏端（新终端）
cd ../screen-web
npm install && npm run dev

# 停止 Docker 服务
cd ../docker
docker compose down
```

### 方式二：手动启动

#### 1. 数据库

```sql
CREATE DATABASE campus_exhibition DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SOURCE docs/design/sql/数据库设计文档.sql;
SOURCE server/sql/init.sql;
```

> 默认管理员：`admin` / `admin123`

#### 2. 启动后端 (server) — :8080

```bash
cd server
# 修改 application.yml 中数据库密码（默认 root/root）
mvn spring-boot:run
# 验证: curl http://localhost:8080/api/ping
```

#### 3. 启动管理后台 (admin-web) — :5173

```bash
cd admin-web
npm install && npm run dev
# 访问 http://localhost:5173，登录: admin / admin123
```

#### 4. 启动大屏展示端 (screen-web) — :5174

```bash
cd screen-web
npm install && npm run dev
# 访问 http://localhost:5174/carousel
```

### 5. 启动人脸识别服务 (face-service) — :8000

```bash
# 1. 安装依赖
cd face-service
pip install -r requirements.txt

# 2. 设置密钥（必填）。密钥在 key/arcface_key.txt
# Bash / Git Bash:
export ARCSOFT_APP_ID=AppIdYOUR_APP_ID   # 从 ArcSoft 官网申请
export ARCSOFT_SDK_KEY=YOUR_SDK_KEY
# 可选：识别阈值（默认 0.80，适合生活照；证件照可设 0.82）
export FACE_THRESHOLD=0.80

# Windows CMD 写法：
# set ARCSOFT_APP_ID=YOUR_APP_ID
# set ARCSOFT_SDK_KEY=YOUR_SDK_KEY

# 3. 启动（默认 DLL 路径 .\x64\Realease\libarcsoft_face_engine.dll）
uvicorn main:app --host 0.0.0.0 --port 8000
# 或: python -m uvicorn main:app --host 0.0.0.0 --port 8000

# 4. 验证
curl http://localhost:8000/health
# 预期: {"status":"ok","service":"face-service","sdk":"arcsoft","modelVer":"arcsoft-v3.0"}
# API 文档: http://localhost:8000/docs
```

**DLL 文件位置：** `face-service\x64\Realease\`

| DLL | 说明 |
|-----|------|
| `libarcsoft_face_engine.dll` | 核心引擎（主 DLL，导出 `ASF*` API 函数） |
| `libarcsoft_face.dll` | 加载器 DLL（引擎依赖） |
| `opencv_core249.dll` 等 8 个 | OpenCV 依赖库 |

> **关键注意：**
> - 仅 Windows x64 可用，SDK 通过 `WinDLL`（`__stdcall`）调用
> - 激活方式为 `ASFOnlineActivation`（需联网一次），APP_ID 中的 `AppId` 前缀由程序自动去除
> - 首次激活成功后再次启动返回 0x16002（已激活），属正常，不报错
> - APPID/SDKKEY 请从 [ArcSoft 官网](https://ai.arcsoft.com.cn) 申请，或查看项目 `key/arcface_key.txt`
> - 若指定 DLL 不存在，可通过 `ARCSOFT_DLL_PATH` 环境变量覆盖

## 环境变量

### admin-web

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_API_BASE` | `/api` | 后端 API 前缀（开发环境通过 Vite proxy 转发到 :8080） |
| `VITE_APP_TITLE` | `校园展览馆 · 管理后台` | 页面标题 |

### screen-web

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_API_BASE` | `/api` | 后端 API 前缀 |
| `VITE_APP_TITLE` | `校园展览馆 · 大屏展示` | 页面标题 |
| `VITE_CAMERA_FACING_MODE` | `environment` | 摄像头方向：`environment`（外接/后置）、`user`（前置）、`auto`（任意） |
| `VITE_CAMERA_DEVICE_ID` | — | 指定摄像头设备 ID（exact 匹配，不配则按 facingMode 自动选择） |

### server (application.yml)

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_HOST` | `localhost` | MySQL 主机 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_USER` | `root` | MySQL 用户 |
| `MYSQL_PASSWORD` | `root` | MySQL 密码 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | `redis123` | Redis 密码 |
| `JWT_SECRET` | `change-me-in-production-use-256-bit-key` | JWT 签名密钥 |
| `app.face.service-url` | `http://localhost:8000` | 人脸服务地址 |
| `app.face.quality-threshold` | `0.6` | 录入质量分门槛（当前 SDK 免费版不返回质量分，实际无效） |
| `app.face.encrypt-key` | `change-me-in-production` | 人脸特征 AES 加密密钥（生产环境必须覆盖） |
| `app.face.store-original-image` | `false` | 是否保留录入原图 |

### face-service (Python 环境变量)

| 变量 | 默认值 | 说明 |
|---|---|---|
| `ARCSOFT_APP_ID` | — | 虹软 APPID（含 `AppId` 前缀，服务自动去掉） |
| `ARCSOFT_SDK_KEY` | — | 虹软 SDKKEY |
| `ARCSOFT_DLL_PATH` | `.\x64\Realease\libarcsoft_face_engine.dll` | 虹软引擎 DLL 路径 |
| `FACE_THRESHOLD` | `0.80` | 识别比对的置信度阈值（生活照推荐 0.80，证件照 0.82） |

## API 接口清单

### 认证 (Auth)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 匿名 | 登录，返回 JWT + 用户信息 |

### 工作台 (Dashboard)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/dashboard/stats` | 三角色 | 统计卡片 + 分类占比 + 审核流转 + 待办 |

### 校友管理 (Alumni)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/alumni` | 三角色 | 分页查询（姓名/学号/学院/届别/身份/状态） |
| GET | `/api/alumni/{id}` | 三角色 | 校友详情 |
| POST | `/api/alumni` | 三角色 | 新增校友 |
| PUT | `/api/alumni/{id}` | 三角色 | 编辑校友 |
| DELETE | `/api/alumni/{id}` | admin | 删除校友 |
| GET | `/api/alumni/template` | admin/academic | 下载 Excel 导入模板 |
| POST | `/api/alumni/import` | admin/academic | 批量导入 Excel |

### 资料档案 (Archive)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/archive` | 三角色 | 分页查询（标题/分类/状态/学院） |
| GET | `/api/archive/{id}` | 三角色 | 档案详情（含媒体列表） |
| POST | `/api/archive` | 三角色 | 新增档案 |
| PUT | `/api/archive/{id}` | 三角色 | 编辑档案 |
| DELETE | `/api/archive/{id}` | admin/academic | 删除档案 |
| POST | `/api/archive/{id}/media` | 三角色 | 上传媒体到档案 |
| DELETE | `/api/archive/{aid}/media/{mid}` | 三角色 | 删除媒体 |
| PUT | `/api/archive/{id}/media/sort` | 三角色 | 媒体排序 |

### 审核中心 (Audit)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| PUT | `/api/audit/{id}/submit` | 三角色 | 提交审核 (draft→pending_college) |
| GET | `/api/audit/college/todo` | college | 学院待办列表 |
| GET | `/api/audit/academic/todo` | academic | 教务处待办列表 |
| PUT | `/api/audit/{id}/college/approve` | college | 学院通过 |
| PUT | `/api/audit/{id}/college/reject` | college | 学院驳回（意见必填） |
| PUT | `/api/audit/{id}/academic/approve` | academic | 教务处通过 |
| PUT | `/api/audit/{id}/academic/reject` | academic | 教务处驳回（意见必填） |
| GET | `/api/audit/{id}/logs` | 三角色 | 审核记录 |

### 资料库运营 (Operation) — admin only

| 方法 | 路径 | 说明 |
|---|---|---|
| PUT | `/api/operation/archive/{id}/publish` | 上架 |
| PUT | `/api/operation/archive/{id}/unpublish` | 下架 |
| PUT | `/api/operation/archive/{id}/top` | 置顶 |
| PUT | `/api/operation/archive/{id}/recommend` | 推荐 |
| PUT | `/api/operation/archive/{id}/sort` | 排序 |
| GET/POST | `/api/operation/carousel` | 轮播方案 CRUD |
| GET/PUT/DELETE | `/api/operation/carousel/{id}` | 单个方案 |
| POST | `/api/operation/carousel/{id}/item` | 添加轮播项 |
| DELETE | `/api/operation/carousel/{cid}/item/{iid}` | 移除轮播项 |
| PUT | `/api/operation/carousel/{id}/items/sort` | 轮播项排序 |

### 大屏公开 (Screen) — 匿名

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/screen/carousel` | 默认轮播方案及内容 |
| GET | `/api/screen/wall` | 校友墙分页 |
| GET | `/api/screen/alumni/{id}` | 校友成长轨迹 |
| GET | `/api/screen/search` | 全文搜索 |

### 人脸识别 (Face)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/face/enroll` | admin | 录入校友人脸 |
| POST | `/api/face/recognize` | 匿名 | 大屏识别（返回 HIT/NO_MATCH/DEGRADED） |

### 媒体 (Media)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/media/upload` | 三角色 | 上传媒体文件 |
| GET | `/api/media/{id}` | 三角色 | 媒体详情 |
| GET | `/api/public/media/{id}` | 匿名 | 公开访问（仅 published） |
| GET | `/api/public/thumb/{id}` | 匿名 | 公开访问缩略图 |

### 字典 (College / Major / Category)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/api/college/all` | 三角色 | 所有学院列表 |
| GET/POST/PUT | `/api/college[/{id}]` | admin | 学院 CRUD |
| GET | `/api/major/by-college` | 三角色 | 按学院查专业 |
| GET/POST/PUT | `/api/major[/{id}]` | admin | 专业 CRUD |
| GET | `/api/category/all` | 三角色 | 资料分类列表 |

### 系统管理 (User / Log) — admin only

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/api/user` | 用户列表 / 新增 |
| PUT/DELETE | `/api/user/{id}` | 编辑 / 删除 |
| PUT | `/api/user/{id}/reset-password` | 重置密码 |
| PUT | `/api/user/{id}/toggle` | 启停用户 |
| GET | `/api/log/oper` | 操作日志分页 |
| GET | `/api/log/login` | 登录日志分页 |

## Chrome Kiosk 全屏部署（一体机大屏）

```bash
# Windows 一体机，Chrome Kiosk 模式
"C:\Program Files\Google\Chrome\Application\chrome.exe" \
  --kiosk \
  --no-first-run \
  --disable-infobars \
  --disable-session-crashed-bubble \
  --autoplay-policy=no-user-gesture-required \
  --unsafely-treat-insecure-origin-as-secure=http://<server-ip>:5174 \
  http://<server-ip>:5174/carousel
```

> 生产环境建议使用 nginx 反向代理，统一 80/443 端口，启用 HTTPS（摄像头需要安全上下文）。

## 端到端冒烟测试链路

```
1. admin 登录后台 (admin/admin123)
2. 校友管理 → 新增校友「张三」(学号 2024001, 计算机学院)
3. 资料档案 → 新建档案「ACM 金牌」(绑定张三, 分类荣誉)
4. 档案编辑页 → 上传奖状图片 + 现场照片
5. 档案列表 → 提交审核 (draft → pending_college)
6. 审核中心(college) → 学院通过 (pending_college → pending_academic)
7. 审核中心(academic) → 教务处通过 (pending_academic → approved)
8. 资料库运营(admin) → 上架 (approved → published)
9. 轮播配置 → 创建方案 → 添加档案到轮播池
10. 大屏 /carousel → 确认轮播正常显示，右下角"人脸识别就绪"指示灯亮起
11. 人脸录入 → 校友列表点击"人脸"按钮 → 上传张三正面照 → 录入成功
12. 大屏 → 张三站到摄像头前 → 自动识别 → "欢迎，张三！" → 专属轨迹轮播 → 回默认轮播（他人站到摄像头前可切换）
```

## 已知遗留问题 & 二期优化

### 待完善

1. **人脸服务**：✅ 已集成 ArcSoft 虹软 SDK v4.x（WinDLL + ASFOnlineActivation），Python/Java 数据格式已对齐（List&lt;Float&gt;），管理后台已支持人脸录入。**限制：仅 Windows x64 可用**，非 Windows 环境无法启动 SDK
3. **视频时长**：`FileStorageService` 未集成 FFprobe/JAVE2，视频时长字段为 null
4. **Redis 缓存**：大屏 ScreenService 使用本地 Caffeine，已预留 Redis 接口但未实现

### 性能优化

5. **前端分包**：admin-web Element Plus CSS 单 chunk 358KB（gzip 48KB），可配置 `manualChunks` 按需拆分
6. **字体子集化**：HarmonyOS_SansSC 字体文件 ~8.4MB，建议用 font-spider 或子集工具压缩
7. **图片裁剪**：头像上传建议增加前端裁剪（cropper.js），统一尺寸减少带宽

### 安全加固

8. **XSS 防护**：部分输入字段（summary、content）未做 HTML 过滤
9. **文件类型深度检测**：`FileStorageService` 仅校验扩展名白名单，未做文件头魔数校验
10. **Token 刷新**：当前 JWT 24h 固定过期，建议加 refresh_token 机制

### 运维

11. **完整容器化**：MySQL/Redis/MinIO 已 Docker 化，Spring Boot 和前端尚未容器化
12. **健康检查**：仅 `/api/ping` 连通 DB，可加 `/health`（含磁盘/内存/face-service 状态）
13. **日志收集**：操作日志写 DB，应用日志建议接入 ELK/Loki

### 功能增强

14. **通知**：审核驳回/通过无站内通知或邮件
15. **批量操作**：校友/档案列表无批量删除/批量审核
16. **数据导出**：仅支持 Excel 导入，无导出功能
17. **大屏动效**：轮播过渡可增加更多动效选项（3D 翻转/粒子消散）
18. **多语言**：大屏英文副标题硬编码，可抽为 i18n
19. **人脸录入前端**：✅ 已在管理后台校友列表加入人脸录入按钮和上传弹窗

## 迭代记录

> 详细迭代记录见 [docs/迭代记录.md](docs/迭代记录.md)

| 版本 | 日期 | 阶段 | 内容 |
|---|---|---|---|
| v0.1.0 | 2026-06-23 | 阶段0 | 项目初始化 — 工程骨架、文档与公共资源 |
| v0.2.0 | 2026-06-23 | 阶段1 | 后端基础设施 — 通用组件、安全认证(JWT)、数据库初始化 |
| v0.3.0 | 2026-06-23 | 阶段2 | 核心业务 — 校友/档案CRUD、审核状态机(draft→published)、运营模块 |
| v0.4.0 | 2026-06-23 | 阶段3+4 | 大屏只读API(轮播/校友墙/搜索) + 人脸识别服务(ArcSoft SDK集成) |
| v0.5.0 | 2026-06-23 | 阶段5.1-5.4 | 管理后台 — Vue3 完整实现（登录/仪表盘/校友/档案/审核/运营/系统管理） |
| v0.6.0 | 2026-06-24 | 阶段5.5-5.7 | 大屏展示端 — Vue3 深色科技风实现（轮播/校友墙/搜索/人脸覆盖层） |
| v0.7.0 | 2026-06-24 | 阶段6 | 系统管理与收尾 — 仪表盘、日志、账号管理 |
| v0.8.0 | 2026-06-24 | 优化 | 特征编码统一(base64)、人脸识别UI优化、上传与审核Bug修复 |
