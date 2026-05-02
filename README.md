# 🐾 课堂宠物乐园 (ClassPet)

一款面向中小学教师的课堂行为管理系统——以"宠物养成"为情感化载体，将学生的课堂表现转化为宠物食物积累与等级成长。让课堂管理从"奖惩压力"变成"成长陪伴"。

> 演示激活码（不区分大小写）：`memory_clear`

---

## ✨ 功能一览

| 模块 | 功能 |
|------|------|
| **认证系统** | 注册 / 登录 / 激活码 / Token 验证 |
| **学生管理** | 添加 / 编辑 / 删除 / 批量导入 / 随机分配宠物 |
| **宠物系统** | 10 种宠物可选 · 等级 Lv.0~Lv.10 · 进度条实时显示 |
| **加减分** | 评分项自由配置 · 分数历史可追溯 · 支持撤销 |
| **道具商店** | 虚拟装备（头饰/衣饰/配饰/特效）· 宠物更换卡 |
| **光荣榜** | 食物排名 + 总积分排名 · 🥇🥈🥉 奖牌展示 |
| **学生明细** | 积分明细 + 道具使用记录双 Tab |
| **主题切换** | 10 套主题色，登录后全局生效 |

---

## 🏗 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Pinia + Vue Router + Axios |
| 后端 | Spring Boot 3.2 + Spring Security + JWT |
| 数据库 | SQLite（随 JAR 自带，零配置） |
| 容器 | Docker / Docker Compose |

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
# 构建
mvn clean package -DskipTests

# 运行（数据存储在当前目录 ./data/）
java -jar backend/target/class-pet-garden-1.0.0.jar
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
│   │   ├── controller/       # REST API 控制器
│   │   ├── service/         # 业务逻辑
│   │   ├── repository/      # JPA 数据访问
│   │   ├── entity/          # 数据库实体
│   │   ├── dto/             # 数据传输对象
│   │   └── config/          # 安全配置 / CORS
│   └── src/main/resources/
│       ├── application.properties
│       └── schema.sql       # 建表语句
│
├── frontend/                 # Vue 3 前端
│   ├── src/
│   │   ├── api/             # Axios 实例封装
│   │   ├── views/           # 页面组件
│   │   ├── components/      # 通用组件（PetCard、ScoreModal 等）
│   │   ├── stores/          # Pinia 状态管理
│   │   └── router/          # 路由守卫
│   └── package.json
│
├── data/                    # SQLite 数据文件（git 忽略）
├── Dockerfile
├── docker-compose.yml
├── SPEC.md                  # 项目规格说明书
└── README.md
```

---

## 🔑 核心 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册账号 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/activate` | 激活码激活 |
| GET | `/api/auth/validate` | 验证 Token 有效性 |
| GET | `/api/students` | 获取学生列表 |
| POST | `/api/students` | 添加学生 |
| PUT | `/api/students/{id}` | 编辑学生 |
| DELETE | `/api/students/{id}` | 删除学生 |
| POST | `/api/students/{id}/adopt` | 领养宠物 |
| POST | `/api/students/assign-pets` | 随机分配宠物 |
| POST | `/api/students/{id}/score` | 加减分 |
| GET | `/api/score-items` | 评分项列表 |
| POST | `/api/score-items` | 添加评分项 |
| DELETE | `/api/score-items/{id}` | 删除评分项 |
| GET | `/api/history` | 积分历史（支持 studentId/from/to 过滤） |
| DELETE | `/api/history/{id}` | 撤销积分记录 |
| GET | `/api/pets` | 宠物库（10种宠物） |
| GET | `/api/shop/items` | 商店商品列表 |
| POST | `/api/shop/exchange` | 兑换商品 |
| POST | `/api/shop/gift` | 赠送道具 |
| GET | `/api/settings` | 获取系统设置 |
| PUT | `/api/settings` | 更新系统设置 |
| GET | `/api/leaderboard/*` | 光荣榜排名 |

---

## 🎨 宠物等级

| 等级 | 所需食物 | 标签 |
|------|---------|------|
| Lv.0 | 0 | 🐣 幼崽期 |
| Lv.1 | 10 | 🐥 幼崽期 |
| Lv.2 | 20 | 🐤 少年期 |
| Lv.3 | 30 | 🐈 少年期 |
| Lv.4 | 40 | 🐱 成年期 |
| Lv.5 | 50 | 🐱 成年期 |
| Lv.6~Lv.9 | 60~90 | ⭐ 成长期 |
| Lv.10 | 100 | 🏆 传说期 |

---

## ⚙️ 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | `8080` |
| `spring.datasource.url` | 数据库路径 | `./data/classpet.db` |
| `jwt.secret` | JWT 签名密钥 | 内置默认（请自行修改） |
| `jwt.expiration` | Token 有效期 | 7 天 |

---

## 📝 版本历史

- **v1.0.21** - 清理测试脚本，添加 .gitignore 规则
- **v1.0.20** - 修复路由守卫无限重定向
- **v1.0.18** - 简化 Token 校验逻辑
- **v1.0.17** - 新增 Token 验证端点，防止旧 Token 残留导致未登录可访问
- **v1.0.16** - 修复 LocalDateTime 时间戳解析问题
- ...（详见 GitHub Releases）

---

> 📌 **演示激活码**：`memory_clear`