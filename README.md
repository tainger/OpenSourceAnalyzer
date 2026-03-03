# AI Source Code Analyzer

一个基于 AI 的开源代码分析工具，支持架构分析、源码走读、错误堆栈分析和 AI 智能问答。

## 功能特性

- 📦 GitHub 仓库克隆与管理（持久化存储）
- 🏗️ 智能架构分析
- 📖 源码走读与理解（树形目录 + 文件搜索）
- 🐛 错误堆栈分析与源码定位
- 🤖 AI 助手智能问答（支持百炼 LLM 集成）
- 🔍 内存向量搜索与余弦相似度匹配
- 💾 仓库元数据持久化

## 技术栈

### 后端
- Java 17+
- Spring Boot 3.x
- JGit (Git 操作)
- Jackson (JSON 处理)
- 内存向量存储

### 前端
- React 18
- TypeScript
- Tailwind CSS
- Axios
- Vite

### AI 集成
- 阿里云百炼 API (DashScope)
- 支持 qwen-turbo 等模型
- 可选：本地轻量级 Embedding

## 前置要求

在开始之前，请确保你已经安装了以下软件：

- **Java 17** 或更高版本
- **Maven 3.8+**
- **Node.js 18+** 和 **npm**

## 快速开始

### 方式一：手动启动

#### 1. 启动后端服务

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

#### 2. 启动前端服务

打开新的终端窗口：

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动。

## 配置说明

### 百炼 LLM API 配置

要启用完整的 AI 功能，请配置百炼 API Key：

```bash
# 方式一：环境变量
export DASHSCOPE_API_KEY=your-api-key-here

# 方式二：修改 application.yml
# 编辑 backend/src/main/resources/application.yml
```

获取 API Key：访问 [阿里云百炼控制台](https://bailian.console.aliyun.com/)

### 后端配置 (application.yml)

```yaml
analyzer:
  repos:
    base-dir: ./data/repos  # 仓库存储目录（项目本地）
  dashscope:
    api-key: ${DASHSCOPE_API_KEY:}  # 百炼 API Key
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    model: qwen-turbo
```

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
3. **新功能**：使用搜索框快速查找文件
4. **新功能**：使用树形目录浏览文件
5. 点击文件查看：
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
   - 相关源码片段
   - 可能的修复方案

### 5. AI 助手

1. 在仓库列表中选择一个已克隆的仓库
2. 点击"🤖 AI 助手"按钮
3. 输入问题，例如：
   - "分析一下这个项目的架构"
   - "分析 Xxx 类的源码"
   - "查找与用户认证相关的代码"

## 项目结构

```
open_source_analysis/
├── backend/                    # Java 后端
│   ├── data/                   # 数据目录
│   │   └── repos/             # 克隆的仓库存储
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/ai/analyzer/
│   │   │   │       ├── controller/      # REST 控制器
│   │   │   │       ├── service/         # 业务逻辑
│   │   │   │       ├── model/           # 数据模型
│   │   │   │       ├── embedding/       # 向量存储与搜索
│   │   │   │       ├── git/             # Git 操作 + 持久化
│   │   │   │       ├── parser/          # 代码解析 + 类名搜索
│   │   │   │       └── config/          # 配置
│   │   │   └── resources/
│   │   │       └── application.yml      # 配置文件
│   │   └── test/
│   └── pom.xml
├── frontend/                   # React 前端
│   ├── src/
│   │   ├── components/         # 组件
│   │   ├── pages/              # 页面
│   │   │   ├── RepositoryList.tsx    # 仓库列表
│   │   │   ├── ArchitectureAnalysis.tsx # 架构分析
│   │   │   ├── CodeWalkthrough.tsx     # 源码走读（树形目录 + 搜索）
│   │   │   ├── ErrorStackAnalysis.tsx  # 错误堆栈分析
│   │   │   └── ChatDialog.tsx          # AI 助手
│   │   ├── services/           # API 服务
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   └── tailwind.config.js
├── start.sh                    # 快速启动脚本
└── README.md
```

## API 文档

### 仓库管理

- `POST /api/repositories/clone` - 克隆并分析仓库
- `GET /api/repositories` - 获取所有仓库（支持持久化）
- `GET /api/repositories/{id}` - 获取指定仓库
- `DELETE /api/repositories/{id}` - 删除仓库

### 分析功能

- `GET /api/analysis/architecture/{repoId}` - 架构分析
- `GET /api/analysis/walkthrough/{repoId}` - 源码走读
- `POST /api/analysis/error-stack` - 错误堆栈分析
- `GET /api/analysis/files/{repoId}` - 获取仓库文件列表

### AI 聊天

- `POST /api/analysis/chat` - AI 助手对话

## 核心功能详解

### 向量搜索

实现了完整的向量相似度搜索：
- 余弦相似度计算
- 混合搜索策略（70% 向量 + 30% 关键词）
- 内存向量存储，无需外部数据库

### 仓库持久化

- 仓库元数据保存为 `repo-metadata.json`
- 启动时自动加载已有仓库
- 支持智能推断缺失的元数据

### 智能类名搜索

- 从错误堆栈自动提取类名
- 在仓库中搜索对应源文件
- 展示报错位置附近的代码片段

### 树形文件目录

- 将平铺文件转换为树形结构
- 支持目录展开/折叠
- 直观的图标和层级缩进

### 文件搜索

- 实时搜索框
- 不区分大小写匹配
- 搜索结果保持树形结构

## 故障排除

### 端口被占用

修改以下配置文件中的端口：
- 后端：`backend/src/main/resources/application.yml`
- 前端：`frontend/vite.config.ts`

### AI 回答不智能

确保已正确配置百炼 API Key：
```bash
echo $DASHSCOPE_API_KEY
```

如果没有配置，系统会使用预设回答模式。

### 仓库丢失

仓库数据保存在 `backend/data/repos/` 目录下，重启后端不会丢失数据。

## 未来计划

- [ ] 支持更多编程语言的深度解析
- [ ] 添加代码复杂度分析
- [ ] 支持 Git 历史分析
- [ ] 添加代码审查建议功能
- [ ] 支持向量数据持久化
- [ ] 集成更多 LLM 提供商

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT



