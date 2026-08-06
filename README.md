# 🐾 课堂宠物乐园 (ClassPet)

一款面向中小学教师的课堂行为管理系统——以"宠物养成"为情感化载体，将学生的课堂表现转化为宠物食物积累与等级成长。让课堂管理从"奖惩压力"变成"成长陪伴"。

> 演示激活码（不区分大小写）：`memory_clear`

---

## ✨ 功能一览

| 模块 | 功能 |
|------|------|
| **朗读系统** | 服务端 Edge TTS（中文 zh-CN-XiaoxiaoNeural / 英文 en-US-JennyNeural），连续重复字自动插入顿号避免声调压平 |
| **认证系统** | 教师注册 / 登录 / 激活码 / Token 验证 / Token 滑动过期 / **学生独立密码体系**（学号登录 + 强制首次改密） |
| **学生管理** | 添加 / 编辑 / 删除 / 随机分配宠物 / **单个 + 批量密码重置** / 学号按教师前缀生成 |
| **宠物系统** | 10 种宠物可选 · 等级 Lv.1~Lv.6 · 进度条实时显示 · 各等级独立视觉特效 |
| **宝可梦系统** | 1025 种基础形态 + 多形态（Mega /阿罗拉 /伽勒尔 /超极巨化）· 精灵球领取 · 食物进化（进度条实时显示） · 道具进化 · 进化道具商店 · 代表宝可梦设置 · 自动按教师类型池抽取 |
| **加减分** | 评分项自由配置 · 分数历史可追溯 · 支持撤销 |
| **道具商店** | 38 种虚拟装备（头饰/衣饰/配饰/特效/光环）· 宠物更换卡 · 精灵球 · 17 种进化道具（水/火/叶/月/雷/冰/日/黑奇/联系绳/王者之证/护具/龙鳞/升级数据/可疑补丁/亲密度进化石 等）· **学生间赠送 + 教师端装备** |
| **光荣榜** | 食物排名 + 总积分排名 · 🥇🥈🥉 奖牌展示 · 6级徽章样式 |
| **学生明细** | 积分明细 + 道具使用记录双 Tab；"送出/接收"视角分别显示"赠送给/来自" |
| **课堂模块** | 语文诗词（朗读/默写/注音/声母韵母翻转卡/诗词填空测验）· 数学四则运算 · 英语字母测验 · **朗读打卡积分**（5种活动，每日每人每项上限10分）· **课堂功能总开关**（`CLASSROOM_ENABLED` 环境变量） |
| **主题切换** | 10 套主题色，登录后全局生效 |
| **学生端** | 独立登录 / 强制改密 / 宠物 + 宝可梦 + 道具面板 / 兑换 / 赠送 / 测验 / 朗读打卡 / 课堂内容 |

---

## 🏗 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Pinia + Vue Router + Axios |
| 后端 | Spring Boot 3.2 + Spring Security + JWT (stateless) + BCrypt |
| 数据库 | SQLite（随 JAR 自带，零配置） |
| 容器 | Docker / Docker Compose |
| 语音 | edge-tts（Python 子进程） |

---

## 🚀 快速部署

### Docker 部署（推荐）

```bash
# 拉取镜像
docker pull namejoe/class-pet-garden:latest

# 创建数据目录（bind mount 持久化数据）
mkdir -p ./data
chmod 777 ./data   # Linux/macOS 确保可写

# 运行容器
docker run -d \
  --name class-pet-garden \
  -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  namejoe/class-pet-garden:latest
```

访问 **http://localhost:8080** 即可使用。

### 课堂功能开关

支持通过环境变量统一控制前后端课堂模块的可见性（默认关闭）：

```bash
# 开启课堂
docker run -e CLASSROOM_ENABLED=true ...

# 关闭课堂
docker run -e CLASSROOM_ENABLED=false ...   # 默认
```

Docker Compose 方式：
```yaml
services:
  class-pet-garden:
    image: namejoe/class-pet-garden:latest
    environment:
      - CLASSROOM_ENABLED=${CLASSROOM_ENABLED:-false}
    ports: ["8080:8080"]
    volumes: ["./data:/app/data"]
```

### Docker Compose 部署

```yaml
version: '3'
services:
  class-pet-garden:
    image: namejoe/class-pet-garden:latest
    container_name: class-pet-garden
    restart: unless-stopped
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data
```

```bash
docker compose up -d
```

### 手动运行（JAR）

```bash
cd backend
# 构建
mvn clean package -DskipTests

# 运行（数据存储在当前目录 ./data/）
java -jar target/class-pet-garden-1.0.0.jar
```

---

## 🐳 本地开发

### 后端

```bash
cd backend
# 首次运行自动建表
mvn spring-boot:run
```

> 后端端口：**8080**

### 前端

```bash
cd frontend
npm install
npm run dev
```

> 前端端口：**5173**（开发时自动代理 `/api` 到后端 8080）

### 课堂功能开关（开发期）

前端通过 `frontend/.env` 控制：
```bash
# frontend/.env
VITE_CLASSROOM_ENABLED=false   # 默认关闭
```

Vite 变量是 **build-time** 烧死的——运行时想改必须重启 Vite 或构建新镜像。

### 完整重启

```powershell
# 后端
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
cd D:\workspace\class-pet-garden\backend; mvn spring-boot:run

# 前端（新窗口）
cd D:\workspace\class-pet-garden\frontend; npm run dev
```

---

## 📁 项目结构

```
class-pet-garden/
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/classpet/
│   │   ├── controller/       # REST API 控制器（Auth/Student/Pet/Shop/Classroom/...）
│   │   ├── service/          # 业务逻辑
│   │   ├── repository/       # JPA 数据访问
│   │   ├── entity/           # 数据库实体（Student 含 passwordHash / mustChangePassword）
│   │   ├── dto/              # 数据传输对象
│   │   ├── security/         # JWT 认证过滤器
│   │   ├── migration/        # 应用启动时自动迁移（学号前缀 + 学生密码迁移）
│   │   └── config/           # 安全配置 / FeaturesConfig / CORS
│   └── src/main/resources/
│       ├── application.properties
│       ├── data/             # 宝可梦数据（species.json / evolution_rules.json）
│       └── static/pokemon/   # 宝可梦图片资源（1367 张 PNG）
│
├── frontend/                 # Vue 3 前端
│   ├── src/
│   │   ├── api/              # Axios 实例封装
│   │   ├── views/            # 页面组件（HomeView / StudentHomeView / ChangePasswordView / ...）
│   │   ├── components/       # 通用组件（PetCard / ScoreModal / ConfirmModal / StudentDetailModal / ...）
│   │   ├── stores/           # Pinia 状态管理（auth / app）
│   │   ├── composables/      # useConfirmModal（全局确认/提示/输入对话框）
│   │   ├── config/           # features.js（前端特性开关）
│   │   └── router/           # 路由 + beforeEach 守卫
│   └── package.json
│
├── data/                     # SQLite 数据文件（git 忽略）
├── Dockerfile
├── docker-compose.yml
├── SPEC.md                   # 项目规格说明书
├── LICENSE                   # MIT License
└── README.md
```

---

## 🔑 核心 API

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 教师注册（username + password + confirmPassword） |
| POST | `/api/auth/login` | 教师登录，返回 JWT |
| POST | `/api/auth/student-login` | 学生登录（学号 + 学生密码），返回 JWT + `mustChangePassword` |
| POST | `/api/auth/student-change-password` | 学生改密（旧 + 新两遍验证） |
| POST | `/api/auth/change-password` | 教师改密 |
| GET | `/api/auth/validate` | 验证 Token 有效性 |
| POST | `/api/auth/activate` | 激活码激活 |

### 学生管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/students` | 获取本班学生列表（教师 token） |
| POST | `/api/students` | 添加学生（可指定初始密码，留空=学号） |
| POST | `/api/students/batch` | 批量添加学生（可指定初始密码） |
| PUT | `/api/students/{id}` | 编辑学生 |
| DELETE | `/api/students/{id}` | 删除学生 |
| POST | `/api/students/{id}/reset-password` | 重置单个学生密码（留空=学号） |
| POST | `/api/students/batch-reset-password` | 批量重置密码（留空=学号） |
| POST | `/api/students/{id}/adopt` | 领养宠物 |
| POST | `/api/students/assign-pets` | 随机分配宠物 |
| POST | `/api/students/{id}/equip` | 装备道具（教师端代装备） |
| DELETE | `/api/students/{id}/equip/{itemId}` | 卸下道具 |

### 学生端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/student/me` | 当前学生信息（含 equippedItems 等） |
| GET | `/api/student/pets` | 学生端宠物库 |
| GET | `/api/student/exchange-history` | 兑换历史（含 giftTo/giftFrom 视角） |
| POST | `/api/student/exchange` | 兑换商品（扣食物） |
| POST | `/api/student/gift-item` | 赠送道具（装备中的道具禁止赠送） |
| POST | `/api/student/equip-item` | 装备/卸下（`{ itemId, equip: "true"/"false" }`） |
| POST | `/api/student/score` | 学生自助积分（课堂用） |
| GET | `/api/student/leaderboard` | 排行榜 |

### 评分 / 历史

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/score-items` | 评分项列表 |
| POST | `/api/score-items` | 添加评分项 |
| DELETE | `/api/score-items/{id}` | 删除评分项 |
| GET | `/api/history` | 积分历史（支持 studentId/from/to 过滤） |
| DELETE | `/api/history/{id}` | 撤销积分记录 |

### 宠物 / 宝可梦

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/pets` | 静态 10 种宠物库 |
| GET | `/api/pets/pokemon` | 宝可梦抽卡池（tier=1 的所有基础形态） |
| GET | `/api/pets/pokemon-library` | 学生当前宝可梦 |
| POST | `/api/student/adopt-pokemon` | 学生端领养宝可梦 |
| POST | `/api/student/feed-pokemon` | 投喂食物（强制 cap 检查：剩余容量 = cap - currentFood） |
| POST | `/api/pokemon/{id}/evolve` | 宝可梦进化 |
| GET | `/api/pokemon/{id}/evolution-options` | 进化选项 |
| POST | `/api/pokemon/{id}/use-evolution-item` | 使用进化道具 |

### 商店 / 兑换 / 赠送

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/shop/items` | 商店商品列表 |
| POST | `/api/shop/exchange` | 教师代学生兑换（扣教师积分） |
| POST | `/api/shop/gift` | 教师转赠道具 |

### 课堂模块（受 `CLASSROOM_ENABLED` 控制）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/classrooms` | 课堂列表 |
| POST | `/api/classrooms` | 创建课堂 |
| PUT | `/api/classrooms/{id}` | 更新课堂 |
| DELETE | `/api/classrooms/{id}` | 删除课堂 |
| POST | `/api/classrooms/{id}/poems` | 添加诗词 |
| DELETE | `/api/classrooms/{id}/poems/{poemId}` | 移除诗词 |
| GET | `/api/student/classroom-config` | 学生端课堂配置 |
| POST | `/api/student/checkin` | 朗读打卡积分 |

### 排行榜 + 设置 + 特性开关

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/students/leaderboard` | 食物榜 |
| GET | `/api/students/leaderboard/total` | 总积分榜 |
| GET | `/api/settings` | 系统设置 |
| PUT | `/api/settings` | 更新系统设置 |
| GET | `/api/features` | 前端特性开关（`{classroomEnabled}`） |

### 迁移监控

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/student-migration-status` | 学生密码迁移进度（需鉴权） |

---

## 🐾 宠物等级

| 等级 | 所需食物 | 标签 |
|------|---------|------|
| Lv.1 | 0-9 | 🥚 幼崽期 |
| Lv.2 | 10-29 | 🌱 少年期 |
| Lv.3 | 30-59 | ⚡ 成长期 |
| Lv.4 | 60-99 | 💎 精英期 |
| Lv.5 | 100-199 | 🔥 大师期 |
| Lv.6 | 200+ | 👑 传说期 |

---

## 🎮 宝可梦系统

### 核心功能

- **精灵球领取**：每月自动发放 1 个精灵球，使用后随机获得一只宝可梦
- **食物进化**：宝可梦食物积累到指定数量自动进化（如妙蛙种子 → 妙蛙草 → 妙蛙花），进度条实时显示还需多少食物
- **道具进化**：使用进化道具（水之石、火之石等）触发特殊进化
- **代表宝可梦**：设置一只宝可梦作为学生头像展示
- **投喂 cap 校验**：后端强制 `单次投喂量 ≤ 剩余容量`（防止前端绕过 UI 大量投喂）
- **超限自动修复**：启动时 `fixOverfedPokemon()` 把超限数据自动截到 cap

### 数据规模

- `species.json`：**1322 条**（1025 基础形态 + 多形态 Mega/地区/超极巨化 + form-only 精灵）
- `static/pokemon/`：**1367 张 PNG** 全部登记
- types 数据：从 PokeAPI 抓取 + 映射中文
- 班级抽卡池：默认 Gen1（#001~#151）tier=1 的所有基础形态（约 151 种）

---

## 🔐 学生独立密码体系

**Q&A 设计决策**：
- 老学生迁移：默认密码 = 学号，强制首次改密
- 教师添加学生：初始密码可输入，留空 = 学号
- 教师重置密码：可填任意值，留空 = 学号
- 学生首次登录：返回 `mustChangePassword: true`，前端 `router.beforeEach` 拦截到 `/change-password` 页

**关键实现**：
- `Student` 实体加 `passwordHash` (BCrypt) + `mustChangePassword` (boolean)
- 迁移用独立 `ApplicationRunner`（`StudentPasswordMigration`），`@Order(MIN_VALUE+1)` 保证在 DDL 完成后跑
- SQLite `ADD COLUMN ... NOT NULL` 不允许 → 用 `ADD COLUMN ... DEFAULT 0` 幂等迁移
- Hibernate session 缓存 → **改密后必须重启后端**新 hash 才生效

---

## ⚙️ 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | `8080` |
| `spring.datasource.url` | 数据库路径 | `./data/classpet.db` |
| `jwt.secret` | JWT 签名密钥 | 内置默认（请自行修改） |
| `jwt.expiration` | Token 有效期 | 7 天（滑动过期） |
| `classroom.enabled` | 课堂功能总开关（环境变量 `CLASSROOM_ENABLED`） | `false` |

### 环境变量

| 变量 | 说明 | 默认 |
|------|------|------|
| `DB_PATH` | SQLite 数据库路径 | `./data/classpet.db` |
| `CLASSROOM_ENABLED` | 课堂模块开关（同时控制前后端） | `false` |
| `VITE_CLASSROOM_ENABLED` | 前端 build 时 fallback | `false` |

---

## 🛡 反代 / 部署提示

### nginx 反向代理

通过 nginx 暴露 class-pet-garden 时（如 8003 → 8080），注意：

1. **同一台机**：`proxy_pass http://127.0.0.1:8080;`
2. **跨机**：用 `upstream classpet_backend { server <后端IP>:8080; keepalive 32; }`
3. **必加头**（Edge TTS WebSocket 流式朗读需要）：
   ```nginx
   proxy_http_version 1.1;
   proxy_set_header Upgrade $http_upgrade;
   proxy_set_header Connection "upgrade";
   proxy_set_header X-Forwarded-Proto $scheme;
   proxy_read_timeout 300s;
   proxy_buffering off;
   ```
4. **SELinux 环境**（CentOS/RHEL）：需要 `sudo semanage port -a -t http_port_t -p tcp 8080` 放行

---

## 📝 版本历史

### v1.5.x（最新）
- **v1.5.0** - 学生独立密码体系：教师端密码重置（单/批）/ 学生端强制改密 / 老学生迁移 / 必须重启后端清 Hibernate session 缓存
- **v1.4.x** - 课堂功能开关：单一 `CLASSROOM_ENABLED` 环境变量通吃前后端（后端运行时注入 `window.__APP_CONFIG__` 到 index.html）；Dockerfile ARG 位置修复
- **v1.4.x** - 宝可梦数据全量补全：species.json 从 166 → 1322 条（基础形态 + 多形态 + types）；静态资源 1367 张图 100% 登记
- **v1.4.x** - 投喂 cap 校验：单次投喂量强制 ≤ 剩余容量；启动自动修复超限宝可梦
- **v1.4.x** - 赠送逻辑修正：`myItems` 前端过滤 `!r.giftFrom`；`giftTo/giftToName` 字段区分"赠送给/来自"视角
- **v1.4.x** - 教师端轮询 + 学生端 `fetchMyInfo`：学生操作后教师端 10 秒内自动更新
- **v1.4.x** - 学号按教师前缀生成（`E8DC-S0001` / `0DAC-S0001`），迁移自动加前缀
- **v1.4.x** - 全局 ConfirmModal 加 `$confirm.prompt` 输入框（替换浏览器原生 `prompt/alert`）

### 早期版本
- **v1.4.1** - 服务端 Edge TTS 朗读（中文/英文），朗读打卡积分系统上线
- **v1.2.0** - 宝可梦系统上线（精灵球领取 / 食物进化 / 道具进化 / 代表宝可梦）
- **v1.1.3** - 扩展等级体系至 6 级（幼崽/少年/成长/精英/大师/传说），各等级独立视觉特效
- **v1.1.2** - 全局 ConfirmModal 替换原生 alert/confirm，统一 UI 风格
- **v1.1.1** - 学生端功能完善（登录/领养宠物/积分明细/道具记录/测验）
- **v1.1.0** - 课堂模块上线（语文诗词/数学四则运算/英语字母翻转卡）
- **v1.0.21** - 清理测试脚本，添加 .gitignore 规则
- **v1.0.20** - 修复路由守卫无限重定向
- ...（详见 GitHub Releases）

---

## 📄 License

MIT License —— 详见 [LICENSE](LICENSE) 文件。

---

> 📌 **演示激活码**：`memory_clear`