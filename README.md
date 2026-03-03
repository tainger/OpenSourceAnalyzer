# AI Source Code Analyzer

一个基于 AI 的开源代码分析工具，支持架构分析、源码走读和错误堆栈分析。

## 功能特性

- 📦 GitHub 仓库克隆与管理
- 🏗️ 智能架构分析
- 📖 源码走读与理解
- 🐛 错误堆栈分析与定位
- 🤖 本地轻量级 BGE Embedding 模型
- 🔍 向量搜索与语义匹配

## 技术栈

### 后端
- Java 17+
- Spring Boot 3.x
- LangChain4j (AI 框架)
- BGE Embedding 模型 (本地)
- ChromaDB (向量数据库)
- JGit (Git 操作)

### 前端
- React 18
- TypeScript
- Tailwind CSS
- Axios
- Vite

## 前置要求

在开始之前，请确保你已经安装了以下软件：

- **Java 17** 或更高版本
- **Maven 3.8+**
- **Node.js 18+** 和 **npm**
- **Docker** 和 **Docker Compose** (用于运行 ChromaDB)

## 快速开始

### 方式一：使用启动脚本（推荐）

```bash
# 克隆项目
git clone <repository-url>
cd open_source_analysis

# 运行启动脚本
./start.sh
```

### 方式二：手动启动

#### 1. 启动 ChromaDB 向量数据库

```bash
docker-compose up -d
```

#### 2. 下载 BGE 模型

下载 BGE 模型到 `~/.ai-analyzer/models/bge-small-zh-v1.5` 目录，或修改 `backend/src/main/resources/application.yml` 中的模型路径。

你可以从 Hugging Face 下载模型：
- [BAAI/bge-small-zh-v1.5](https://huggingface.co/BAAI/bge-small-zh-v1.5)

#### 3. 启动后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

#### 4. 启动前端服务

打开新的终端窗口：

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动。

## 使用指南

### 1. 克隆仓库

1. 打开前端页面 `http://localhost:3000`
2. 在"克隆新仓库"区域输入 GitHub 仓库 URL
3. 可选：指定分支（默认 main）
4. 点击"克隆并分析"

### 2. 架构分析

1. 在仓库列表中选择一个已克隆的仓库
2. 点击"🏗️ 架构分析"按钮
3. 查看：
   - 整体结构
   - 主要模块
   - 技术栈
   - 设计模式
   - 关键文件
   - 建议

### 3. 源码走读

1. 在仓库列表中选择一个已克隆的仓库
2. 点击"📖 源码走读"按钮
3. 从左侧文件列表选择要查看的文件
4. 查看：
   - 文件摘要
   - 代码分段解析
   - 依赖关系

### 4. 错误堆栈分析

1. 点击导航栏的"错误分析"
2. 可选：选择关联的仓库
3. 粘贴错误堆栈信息
4. 点击"分析错误堆栈"
5. 查看：
   - 错误类型
   - 根本原因
   - 可疑位置（带置信度）
   - 可能的修复方案
   - 相关代码

## 项目结构

```
open_source_analysis/
├── backend/                    # Java 后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/ai/analyzer/
│   │   │   │       ├── controller/      # REST 控制器
│   │   │   │       ├── service/         # 业务逻辑
│   │   │   │       ├── model/           # 数据模型
│   │   │   │       ├── embedding/       # Embedding 相关
│   │   │   │       ├── git/             # Git 操作
│   │   │   │       └── parser/          # 代码解析
│   │   │   └── resources/
│   │   │       └── application.yml      # 配置文件
│   │   └── test/
│   └── pom.xml
├── frontend/                   # React 前端
│   ├── src/
│   │   ├── components/         # 组件
│   │   ├── pages/              # 页面
│   │   ├── services/           # API 服务
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   └── tailwind.config.js
├── docker-compose.yml          # ChromaDB 配置
├── start.sh                    # 快速启动脚本
└── README.md
```

## API 文档

### 仓库管理

- `POST /api/repositories/clone` - 克隆并分析仓库
- `GET /api/repositories` - 获取所有仓库
- `GET /api/repositories/{id}` - 获取指定仓库
- `DELETE /api/repositories/{id}` - 删除仓库

### 分析功能

- `GET /api/analysis/architecture/{repoId}` - 架构分析
- `GET /api/analysis/walkthrough/{repoId}` - 源码走读
- `POST /api/analysis/error-stack` - 错误堆栈分析
- `GET /api/analysis/files/{repoId}` - 获取仓库文件列表

## 配置说明

### 后端配置 (application.yml)

```yaml
analyzer:
  repos:
    base-dir: ${user.home}/.ai-analyzer/repos  # 仓库存储目录
  chroma:
    url: http://localhost:8000                   # ChromaDB 地址
    collection-name: source-code                 # 向量集合名称
  bge:
    model-path: ${user.home}/.ai-analyzer/models/bge-small-zh-v1.5  # 模型路径
```

## 故障排除

### ChromaDB 连接失败

确保 Docker 正在运行，并且 ChromaDB 容器已启动：
```bash
docker-compose ps
docker-compose logs chroma
```

### 模型加载失败

确保 BGE 模型文件已正确下载并放置在配置的路径中。

### 端口被占用

修改以下配置文件中的端口：
- 后端：`backend/src/main/resources/application.yml`
- 前端：`frontend/vite.config.ts`
- ChromaDB：`docker-compose.yml`

## 未来计划

- [ ] 支持更多编程语言的深度解析
- [ ] 集成本地 LLM 进行更智能的分析
- [ ] 添加代码复杂度分析
- [ ] 支持 Git 历史分析
- [ ] 添加代码审查建议功能
- [ ] 支持更多向量数据库
- [ ] 添加用户认证和多租户支持

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT
