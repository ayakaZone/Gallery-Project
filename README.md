## 灵图空间协同云库

一个支持多空间管理、标签检索、批量编辑和统计分析的云端图片管理平台。项目采用前后端分离架构，并提供 **传统分层后端** 与 **DDD 领域驱动后端** 两种实现。

## 项目结构

- **yu-picture-backend**：基于 Spring Boot 的传统分层架构后端
- **yu-picture-backend-ddd**：基于 DDD（领域驱动设计）的后端实现
- **yu-picture-frontend**：基于 Vue 3 + Vite 的前端单页应用

## 技术栈概览

- **前端**
  - Vue 3、TypeScript、Vite
  - Vue Router、Pinia
  - Ant Design Vue 组件库
  - ECharts / vue-echarts 可视化统计
  - 图片裁剪、扩图等组件（如 `vue-cropper`）

- **后端（两套后端整体技术类似）**
  - Spring Boot 2.7.x
  - Spring Web、Spring AOP
  - MyBatis-Plus + MySQL（`yu_picture` 数据库）
  - Redis + Spring Session（会话与缓存）
  - Sa-Token 权限认证
  - ShardingSphere 分库分表（按空间分片图片表）
  - 腾讯云 COS 对象存储（图片文件存储）
  - WebSocket 实时通信（如图片编辑协同）
  - Knife4j 接口文档

## 运行前准备

- **环境要求**
  - JDK 11+
  - Maven 3.6+
  - Node.js 18+、npm
  - MySQL 本地数据库，并创建 `yu_picture` 数据库
  - 本地 Redis 服务

- **配置说明（重要）**
  - 后端端口及基础配置参考：
    - `yu-picture-backend/src/main/resources/application.yml`
    - `yu-picture-backend-ddd/src/main/resources/application.yml`
    - 默认端口：**8123**，上下文路径：`/api`
  - 数据库、Redis、分表等配置在 `application.yml` 中，请根据本地环境修改：
    - MySQL 连接地址、用户名密码
    - Redis 地址、密码
  - 对象存储与 AI 能力配置在：
    - `yu-picture-backend/src/main/resources/application-local.yml`
    - `yu-picture-backend-ddd/src/main/resources/application-local.yml`

## 快速开始

### 1. 初始化数据库

- 在 MySQL 中创建数据库：
  - 名称建议：`yu_picture`
- 导入建表 SQL：
  - 传统后端：`yu-picture-backend/sql/create_table.sql`
  - DDD 后端：`yu-picture-backend-ddd/sql/create_table.sql`

### 2. 启动后端（任选其一）

#### 2.1 传统分层后端 `yu-picture-backend`

进入目录并启动：

```bash
cd yu-picture-backend
mvn spring-boot:run
```

默认启动在 `http://localhost:8123/api`。

#### 2.2 DDD 后端 `yu-picture-backend-ddd`

进入目录并启动：

```bash
cd yu-picture-backend-ddd
mvn spring-boot:run
```

同样默认端口为 `8123`，上下文路径为 `/api`，**建议一次只启动一个后端项目**，以避免端口冲突。

#### 2.3 接口文档（Knife4j）

后端启动后，可在浏览器访问（路径可能根据实际配置略有差异）：

- `http://localhost:8123/api/doc.html`

查看所有接口、参数和示例。

### 3. 启动前端 `yu-picture-frontend`

安装依赖并启动开发服务器：

```bash
cd yu-picture-frontend
npm install
npm run dev
```

默认访问地址（以 Vite 默认端口为例）：

- `http://localhost:5173/`

> 如需与后端联调，请确保前端请求的后端基础地址与实际后端地址一致（可在前端 `src/api` / `src/request.ts` 中查看和修改）。

## 主要功能概览

- **用户与权限**
  - 用户注册 / 登录 / 注销
  - 基于 Sa-Token 的登录态管理、权限控制
  - 管理员后台管理入口

- **图片管理**
  - 本地上传图片、链接导入图片
  - 图片基本信息维护：名称、描述、标签、分类等
  - 批量上传与批量编辑（如批量修改标签、空间等）
  - 图片详情查看、预览、放大缩小

- **空间（Space）管理**
  - 创建/编辑/删除空间
  - 为图片分配空间，实现多相册、多项目隔离
  - 空间成员与权限控制（在 DDD 版本中有更清晰的领域建模）

- **智能与工具能力**
  - 图片裁剪、扩图等基础编辑能力
  - 基于外部 AI 能力的图片处理 / 智能推荐（取决于具体密钥配置）

- **统计与分析**
  - 空间图片数量、容量使用情况统计
  - 标签、分类分布分析
  - 用户使用情况与上传趋势分析
  - 通过 ECharts 图表可视化展示

- **分享与协作**
  - 图片/空间分享链接
  - 基于 WebSocket 的协同编辑或实时刷新（视具体实现而定）

## 前后端架构对比说明

- **`yu-picture-backend`（传统分层架构）**
  - 常见的 Controller / Service / Mapper 分层
  - 上手快、结构清晰，适合作为入门或小型项目实践

- **`yu-picture-backend-ddd`（DDD 架构）**
  - 引入领域层、应用层、基础设施层等概念
  - 将领域模型（如用户、空间、图片、空间成员等）进行更清晰的拆分与约束
  - 适合希望学习 DDD 思想、或希望在复杂业务中保持长期可维护性的同学参考

## 开发与构建命令速览

- **前端**
  - 安装依赖：`npm install`
  - 本地开发：`npm run dev`
  - 生产构建：`npm run build`
  - 预览构建：`npm run preview`
  - 类型检查：`npm run type-check`
  - 代码风格：`npm run lint` / `npm run format`

- **后端**
  - 本地运行：`mvn spring-boot:run`
  - 打包构建：`mvn clean package`

## 注意事项

- 此项目仅记录个人学习


