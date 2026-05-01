# 课堂宠物乐园 (ClassPet) — 项目规格说明书

## 1. Concept & Vision

**产品定位**：一款面向中小学教师的课堂行为管理系统，以"宠物养成"为情感化载体，将学生的课堂表现（答题、作业、守纪律等）转化为宠物食物积累与等级成长。

**核心理念**：让课堂管理从"奖惩压力"变成"成长陪伴"——学生看见自己的宠物一点点长大，获得正向激励；教师有了一个可视化、易操作的加减分工具。

**与原版的关键区别**：
- 从单 HTML → 分层架构（Spring Boot API + Vue 3 前端）
- 从 localStorage → 后端持久化存储（SQLite）
- 从纯前端 → 前后端分离，API 可扩展
- 从单用户 → 多班级/多教师支持
- 从纯加分扣分 → 增加行为历史记录与统计
- 从单一平台 → 响应式 PC/移动端适配

---

## 2. Design Language

### 2.1 美学方向
**风格**：温暖治愈系教育工具 —— 类 Notion/Figma 的简洁感，但用暖色调和圆润卡片营造"小宠物乐园"氛围。

### 2.2 色彩系统
| 用途 | 颜色名 | Hex |
|------|--------|-----|
| 主色 | Coral Pink | `#FF6B9D` |
| 辅色 | Deep Purple | `#8B5CF6` |
| 背景渐变 | 粉→蓝斜渐变 | `linear-gradient(135deg, #FCE4EC 0%, #E3F2FD 100%)` |
| 卡片背景 | 纯白 | `#FFFFFF` |
| 文字主色 | 深灰 | `#1F2937` |
| 文字辅色 | 中灰 | `#6B7280` |
| 加分色 | 翠绿 | `#10B981` |
| 扣分色 | 玫红 | `#EF4444` |
| 进度条 | 橙→粉渐变 | `linear-gradient(90deg, #FFB347, #FF6B9D)` |

### 2.3 字体
- 主字体：`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`
- 标题字重：600；正文字重：400
- 表情符号用于宠物/操作图标

### 2.4 间距系统
- 基础单位：4px
- 卡片内边距：20px
- 卡片圆角：16px（大卡片）/ 12px（小卡片）/ 8px（按钮）
- 阴影：`0 2px 8px rgba(0,0,0,0.08)`（默认）/ `0 8px 24px rgba(0,0,0,0.12)`（悬停）

### 2.5 动效哲学
- 卡片悬停：上浮 4px + 阴影加深，过渡 0.2s ease
- 模态框：scale(0.95)→scale(1) + opacity，0.2s ease-out
- 加减分触发：宠物图标 bounce 动画（0.3s）
- 进度条：width 过渡 0.3s ease

---

## 3. Layout & Structure

### 3.1 页面结构
```
/
├── 后端 (Spring Boot)
│   ├── src/main/java/com/classpet/
│   │   ├── controller/     # REST API
│   │   ├── service/        # 业务逻辑
│   │   ├── repository/     # JPA Repository
│   │   ├── entity/         # JPA Entity
│   │   ├── dto/            # 数据传输对象
│   │   └── config/         # 配置（CORS、Security）
│   └── src/main/resources/
│       ├── application.properties
│       └── schema.sql      # SQLite 建表
│
├── 前端 (Vue 3 + Vite + Axios)
│   ├── public/
│   ├── src/
│   │   ├── api/            # axios 封装
│   │   ├── views/          # 页面组件
│   │   ├── components/     # 通用组件
│   │   ├── stores/         # Pinia 状态管理
│   │   └── router/         # 路由
│   └── package.json
│
└── pom.xml
```

### 3.2 页面路由
| 路由 | 页面 |
|------|------|
| `/` | 登录 / 注册 |
| `/activate` | 激活码激活 |
| `/home` | 主界面（宠物卡片墙） |
| `/history` | 加减分历史记录 |
| `/settings` | 老师设置页（学生管理、评分项、主题） |

### 3.3 响应式策略
- Desktop (>768px)：网格 4-5 列宠物卡片
- Tablet (768px)：网格 3 列
- Mobile (<768px)：网格 1-2 列 + 底部快捷操作栏

---

## 4. Features & Interactions

### 4.1 认证系统
- **登录**：用户名 + 密码 → JWT Token → 后续请求携带 Bearer Token
- **注册**：用户名（6+字符，字母数字） + 密码（6+字符） + 确认密码
- **激活码**：演示模式任意格式符合 `XXXX-XXXX-XXXX` 的码可激活

### 4.2 班级与学生管理
- 创建班级（老师可管理多个班级，通过下拉切换）
- 添加学生：单条输入（最多10字）/ 批量粘贴（一行一个）
- 编辑学生姓名、删除学生
- 搜索学生（按姓名过滤卡片墙）
- 一键随机分配宠物（给所有未分配学生）

### 4.3 宠物系统
**宠物库**（10种）：
🐱 小橘猫 | 🐶 柴犬 | 🐰 垂耳兔 | 🐹 金丝熊 | 🦊 小狐狸 | 🐼 熊猫 | 🦁 小狮子 | 🐯 小老虎 | 🐨 考拉 | 🐮 奶牛

**宠物属性**：
- `pet`：宠物对象（id, name, icon）
- `food`：累计食物点数（初始 0，无上限）
- `level`：等级（Lv.0 ~ Lv.10，Lv.N 需要 N×10 食物，即 Lv.1=10, Lv.2=20... Lv.10=100）
- `progress`：当前等级进度百分比（0~100%）

**领养方式**：
1. 逐个领养：点击卡片"点击领养" → 弹出宠物选择弹窗 → 确认
2. 一键分配：在设置页一键为所有未分配学生随机分配宠物

### 4.4 加减分系统
**默认评分项**（可增删改）：
| 类型 | 项目 | 分值 |
|------|------|------|
| ✨ 加分 | 📖 早读打卡 | +1 |
| ✨ 加分 | 💡 答对问题 | +2 |
| ✨ 加分 | 📘 作业优秀 | +3 |
| ✨ 加分 | 🎤 完成背诵 | +2 |
| ✨ 加分 | 🌱 进步明显 | +3 |
| ⚠️ 扣分 | ⏰ 迟到 | -1 |
| ⚠️ 扣分 | 📱 玩手机 | -2 |
| ⚠️ 扣分 | 😴 打瞌睡 | -1 |

**交互**：
- 点击宠物卡片 → 弹出加减分弹窗 → 分加分��/扣分项两组 → 点击项目 → 食物变化 → 弹出确认提示 + 卡片刷新
- 食物最低为 0

### 4.5 加减分历史
- 记录每次操作：学生名、评分项、变化值、操作时间
- 时间轴展示，最新在上
- 统计页面：本周/本月加减分趋势（条形图）

### 4.6 主题切换
- 10 套主题：pink, purple, indigo, blue, cyan, teal, green, yellow, orange, red
- 点击主题 → 全局 CSS 变量 `--theme-color` 变化
- 主题保存至后端，用户重新登录保持

### 4.7 边界状态
| 状态 | 处理 |
|------|------|
| 无学生 | 显示空状态插图 + "去设置添加学生" 链接 |
| 未领养宠物 | 卡片显示 🥚 蛋 + "点击领养" 按钮 |
| 食物为 0 | 进度条为 0，Lv.0 |
| 达到 Lv.10 | 显示"已满级！🏆" |
| 搜索无结果 | 隐藏卡片，显示"未找到该学生" |

---

## 5. Component Inventory

### 5.1 PetCard 宠物卡片
- 状态：未领养 / 已领养 / 悬停
- 显示：等级徽章、宠物图标、学生姓名、进度条、食物数、Lv.×10
- 悬停：transform: translateY(-4px)，阴影加深

### 5.2 ScoreModal 加减分弹窗
- 状态：关闭 / 打开
- 分两组：加分项（绿色高亮）/ 扣分项（红色高亮）
- 点击项目后自动关闭弹窗并刷新卡片

### 5.3 PetSelectModal 宠物选择弹窗
- 5列网格展示10种宠物
- 选中态：背景 `#FFF7E6`，边框 `#FFB347`
- 确认按钮：渐变粉红色

### 5.4 SettingsPanel 设置面板
- 全屏模态框（90%宽，最大600px）
- Tab：账号 / 系统 / 主题 / 评分项 / 学生管理
- 底部固定保存/取消按钮

### 5.5 StudentList 学生列表
- 每行：序号 + 姓名 + 宠物状态 + 编辑/删除按钮
- 内联编辑态：输入框替代文本

---

## 6. Technical Approach

### 6.1 后端
- **框架**：Spring Boot 3.x（Java 17+）
- **数据库**：SQLite（轻量，无需安装，适合本项目）
- **ORM**：Spring Data JPA + Hibernate
- **安全**：JWT（jjwt 库）+ 简单密码 Hash（BCrypt）
- **跨域**：CORS 配置允许前端 dev server

**核心 Entity**：
```
Teacher { id, username, password, systemName, className, theme, activated }
Class { id, name, teacherId }
Student { id, name, classId, petId, petName, petIcon, food, createdAt }
ScoreItem { id, teacherId, icon, name, point }
ScoreHistory { id, studentId, scoreItemName, point, createdAt }
```

**核心 API**：
```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/activate
GET    /api/students
POST   /api/students
PUT    /api/students/{id}
DELETE /api/students/{id}
POST   /api/students/{id}/adopt   { petId }
POST   /api/students/assign-pets  (随机分配)
POST   /api/students/{id}/score   { scoreItemId }
GET    /api/score-items
POST   /api/score-items
DELETE /api/score-items/{id}
GET    /api/history?studentId=&from=&to=
PUT    /api/settings               { systemName, className, theme }
GET    /api/pets                  (宠物库)
```

### 6.2 前端
- **框架**：Vue 3（Composition API）+ Vite
- **状态管理**：Pinia
- **路由**：Vue Router 4
- **HTTP**：Axios（请求拦截器自动附加 JWT）
- **样式**：纯 CSS（无框架，参考原版风格）+ CSS 变量

### 6.3 数据流
1. 登录 → 拿到 JWT Token → 存入 Pinia store + localStorage
2. Axios 拦截器：每次请求附加 `Authorization: Bearer <token>`
3. 所有数据通过 API 获取，不再使用 localStorage 存储业务数据

### 6.4 部署
- 前端：`npm run build` → 输出到 `backend/src/main/resources/static/`
- 后端：`mvn package` → 单 JAR 包，内嵌 SQLite
- 启动：`java -jar app.jar`（默认端口 8080）