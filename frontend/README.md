# 课堂宠物乐园 - 前端

## 项目结构

```
frontend/
├── public/              # 静态公共资源（构建时复制）
├── src/
│   ├── api/            # API 接口调用 (app.js store 封装)
│   ├── components/      # Vue 组件
│   │   ├── ExchangeModal.vue      # 兑换弹窗
│   │   ├── LevelGuide.vue         # 等级说明
│   │   ├── PetCard.vue           # 宠物卡片
│   │   ├── PetSelectModal.vue    # 宠物选择
│   │   ├── ScoreModal.vue        # 评分弹窗
│   │   └── StudentDetailModal.vue # 学生明细
│   ├── router/          # Vue Router 配置
│   ├── stores/          # Pinia 状态管理
│   │   ├── app.js       # 应用状态（学生/宠物/商品等）
│   │   └── auth.js      # 认证状态
│   ├── views/           # 页面视图
│   │   ├── ActivateView.vue      # 激活页
│   │   ├── ExchangeHistoryView.vue # 兑换明细
│   │   ├── HistoryView.vue       # 加减分历史
│   │   ├── HomeView.vue          # 首页
│   │   ├── LeaderboardView.vue   # 光荣榜
│   │   └── LoginView.vue         # 登录页
│   ├── App.vue
│   └── main.js
├── index.html           # Vue 入口 HTML
├── package.json
└── vite.config.js       # Vite 配置（代理 /api 到后端 8080）
```

## 运行命令

```bash
# 安装依赖
npm install

# 开发模式（自动代理 /api 到 localhost:8080）
npm run dev

# 生产构建
npm run build
```

## 接口说明

前端通过 Vite 代理访问后端 API：

| 前端请求 | 后端实际请求 |
|---------|-------------|
| `/api/*` | `http://localhost:8080/api/*` |

后端接口前缀：`/api/`

## 相关文档

- 项目规格说明：`../SPEC.md`
- 后端代码：`../backend/`