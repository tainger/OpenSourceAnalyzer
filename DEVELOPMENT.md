# AI Source Code Analyzer - 开发文档

## 目录
- [项目概述](#项目概述)
- [技术架构](#技术架构)
- [核心模块详解](#核心模块详解)
- [开发环境搭建](#开发环境搭建)
- [API 文档](#api-文档)
- [扩展开发指南](#扩展开发指南)
- [常见问题](#常见问题)

---

## 项目概述

AI Source Code Analyzer 是一个基于 AI 的开源代码分析工具，通过向量搜索和大语言模型（LLM）提供智能代码分析能力。

### 核心功能
1. **GitHub 仓库克隆与管理** - 支持 Git 仓库克隆、元数据持久化
2. **智能架构分析** - 自动识别项目结构、模块、技术栈
3. **源码走读** - 树形目录浏览、文件搜索、代码分段解析
4. **错误堆栈分析** - 自动解析堆栈、定位源码、提供修复建议
5. **AI 智能问答** - 基于上下文的代码对话、类名查询、架构咨询

### 设计理念
- **模块化** - 各功能模块独立，易于扩展和维护
- **可插拔** - 支持多种 LLM 提供商、向量数据库
- **高性能** - 内存向量存储、异步处理、增量分析
- **用户友好** - 简洁的前端界面、清晰的 API 设计

---

## 技术架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend (React)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│  │Repo List │ │Arch Analy│ │Code Walk │ │Error Ana │    │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ REST API
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Backend (Spring Boot)                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    Controllers                         │  │
│  │  RepositoryController  │  AnalysisController          │  │
│  └──────────────────────────────────────────────────────┘  │
│                              │                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                     Services                            │  │
│  │  ChatService  │  RepositoryAnalysisService            │  │
│  └──────────────────────────────────────────────────────┘  │
│                              │                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  Core Components                        │  │
│  │  GitService  │  CodeParserService  │  VectorStore     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      External Services                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  DashScope   │  │  JGit        │  │  File System  │   │
│  │  (LLM API)   │  │  (Git 操作)   │  │  (Repo 存储)  │   │
│  └──────────────┘  └──────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈详情

#### 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | 开发语言 |
| Spring Boot | 3.2.x | Web 框架 |
| JGit | 6.9.0 | Git 操作 |
| Lombok | - | 简化代码 |
| Jackson | - | JSON 处理 |
| Maven | 3.8+ | 构建工具 |

#### 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18.2 | UI 框架 |
| TypeScript | 5.2+ | 类型安全 |
| Tailwind CSS | 3.3 | 样式框架 |
| React Router | 6.21 | 路由管理 |
| Axios | 1.6 | HTTP 客户端 |
| Vite | 5.0 | 构建工具 |

#### AI 集成
- **阿里云百炼 (DashScope)** - 默认 LLM 提供商
- **支持模型** - qwen-turbo, qwen-plus 等
- **Embedding** - 本地轻量级 Embedding (可选)

---

## 核心模块详解

### 1. 项目结构

```
open_source_analysis/
├── backend/
│   ├── src/main/java/com/ai/analyzer/
│   │   ├── config/          # 配置类
│   │   │   └── AnalyzerProperties.java
│   │   ├── controller/      # REST 控制器
│   │   │   ├── RepositoryController.java
│   │   │   └── AnalysisController.java
│   │   ├── service/         # 业务逻辑
│   │   │   ├── ChatService.java
│   │   │   └── RepositoryAnalysisService.java
│   │   ├── model/           # 数据模型
│   │   │   ├── Repository.java
│   │   │   ├── CodeChunk.java
│   │   │   └── dto/         # DTO 类
│   │   ├── embedding/       # 向量存储
│   │   │   ├── VectorStoreService.java
│   │   │   └── EmbeddingService.java
│   │   ├── git/             # Git 操作
│   │   │   └── GitService.java
│   │   ├── parser/          # 代码解析
│   │   │   └── CodeParserService.java
│   │   └── SourceCodeAnalyzerApplication.java
│   └── src/main/resources/
│       └── application.yml
├── frontend/
│   ├── src/
│   │   ├── pages/           # 页面组件
│   │   │   ├── RepositoryList.tsx
│   │   │   ├── ArchitectureAnalysis.tsx
│   │   │   ├── CodeWalkthrough.tsx
│   │   │   ├── ErrorStackAnalysis.tsx
│   │   │   └── ChatDialog.tsx
│   │   ├── services/        # API 服务
│   │   │   └── api.ts
│   │   └── App.tsx
│   └── package.json
└── data/
    └── repos/               # 克隆的仓库存储
```

### 2. 核心模块说明

#### 2.1 GitService - Git 仓库管理

**位置**: `backend/src/main/java/com/ai/analyzer/git/GitService.java`

**功能**:
- 克隆 GitHub 仓库到本地
- 仓库元数据持久化（JSON 文件）
- 仓库列表管理、删除
- 启动时自动加载已有仓库

**关键方法**:
```java
public Repository cloneRepository(String url, String branch);  // 克隆仓库
public Repository getRepository(String id);                     // 获取仓库
public Map<String, Repository> getAllRepositories();           // 获取所有仓库
public void deleteRepository(String id);                        // 删除仓库
```

**数据存储**:
- 仓库克隆到: `data/repos/{repo-id}/`
- 元数据存储: `data/repos/{repo-id}/repo-metadata.json`

---

#### 2.2 CodeParserService - 代码解析

**位置**: `backend/src/main/java/com/ai/analyzer/parser/CodeParserService.java`

**功能**:
- 遍历仓库文件，过滤非源码文件
- 将代码文件分块（Code Chunk）
- 识别测试文件并过滤
- 支持多种编程语言

**关键方法**:
```java
public List<CodeChunk> parseRepository(String repoId, String localPath);
public List<String> listFiles(String localPath);
public List<String> searchFilesByClassName(String localPath, String className);
public String readFile(String localPath, String relativePath);
```

**代码分块策略**:
| 语言 | 分块大小 | 重叠行数 |
|------|---------|---------|
| Java, Kotlin, Scala, C# | 100 行 | 5 行 |
| Python, Ruby, PHP | 80 行 | 5 行 |
| JavaScript, TypeScript | 120 行 | 5 行 |
| 其他 | 100 行 | 5 行 |

**测试文件过滤**:
- 目录: `test/`, `tests/`
- 文件名: `*Test.java`, `*Tests.java`, `test_*.py`, `*.test.ts` 等

---

#### 2.3 VectorStoreService - 向量存储与搜索

**位置**: `backend/src/main/java/com/ai/analyzer/embedding/VectorStoreService.java`

**功能**:
- 内存向量存储（ConcurrentHashMap）
- 余弦相似度计算
- 混合搜索策略（70% 向量 + 30% 关键词）
- 按仓库隔离向量数据

**关键方法**:
```java
public void addCodeChunk(CodeChunk chunk);
public void addCodeChunks(List<CodeChunk> chunks);
public List<CodeChunk> search(String query, String repositoryId, int limit);
public void deleteByRepositoryId(String repositoryId);
```

**相似度计算**:
```
combinedScore = 0.7 * cosineSimilarity + 0.3 * keywordScore
```

**关键词评分**:
- 文件名匹配: +0.5 分/词
- 内容匹配: +0.3 分/词
- 最高 1.0 分

---

#### 2.4 ChatService - AI 对话服务

**位置**: `backend/src/main/java/com/ai/analyzer/service/ChatService.java`

**功能**:
- 检测用户输入是否为错误堆栈
- 构建智能 Prompt（包含项目信息、相关代码）
- 调用百炼 LLM API
- 错误堆栈优先处理
- 类名查询自动文件查找

**关键方法**:
```java
public String chat(String repositoryId, String message);
private String buildPrompt(Repository repository, String userMessage);
private boolean isErrorStack(String message);
private String analyzeErrorStack(String repositoryId, String errorStack);
private String extractClassName(String userMessage);
```

**Prompt 构建流程**:
1. 添加项目基本信息
2. 检测是否为错误堆栈
   - 是: 调用 `RepositoryAnalysisService.analyzeErrorStack()`，添加分析结果和相关代码
   - 否: 检测是否为类名查询
     - 是: 查找文件，添加文件内容
     - 否: 添加项目文件列表、README、相关代码片段

---

#### 2.5 RepositoryAnalysisService - 综合分析服务

**位置**: `backend/src/main/java/com/ai/analyzer/service/RepositoryAnalysisService.java`

**功能**:
- 克隆并分析仓库（一站式服务）
- 架构分析
- 源码走读
- 错误堆栈深度分析

**关键方法**:
```java
public Repository cloneAndAnalyze(String url, String branch);
public ArchitectureAnalysisResponse analyzeArchitecture(String repoId);
public CodeWalkthroughResponse walkthroughCode(String repoId, String filePath);
public ErrorStackAnalysisResponse analyzeErrorStack(String repoId, String errorStack);
```

**错误堆栈分析策略**:
1. 解析堆栈，提取类名、方法名、行号
2. 按三级策略查找文件:
   - 包路径匹配 (如: `com/example/MyClass.java`)
   - 简单文件名匹配 (如: `MyClass.java`)
   - 模糊匹配
3. 读取相关代码片段
4. 生成修复建议

---

## 开发环境搭建

### 前置要求

- **Java 17** 或更高版本
- **Maven 3.8+**
- **Node.js 18+** 和 **npm**
- **Git** (用于克隆仓库)

### 后端开发环境

#### 1. 克隆项目
```bash
git clone <your-repo-url>
cd open_source_analysis
```

#### 2. 配置环境变量（可选）
```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

或者修改 `backend/src/main/resources/application.yml`:
```yaml
analyzer:
  dashscope:
    api-key: your-api-key-here
```

#### 3. 编译后端
```bash
cd backend
mvn clean compile
```

#### 4. 运行后端
```bash
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动。

### 前端开发环境

#### 1. 安装依赖
```bash
cd frontend
npm install
```

#### 2. 启动开发服务器
```bash
npm run dev
```

前端将在 `http://localhost:3000` 启动。

### 使用 Docker（可选）

项目提供了 `docker-compose.yml`，可以一键启动：
```bash
docker-compose up -d
```

---

## API 文档

### 基础信息
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **CORS**: 允许所有来源（开发环境）

---

### 1. 仓库管理 API

#### 1.1 克隆并分析仓库
```
POST /api/repositories/clone
```

**请求体**:
```json
{
  "url": "https://github.com/user/repo.git",
  "branch": "main"
}
```

**响应 (200 OK)**:
```json
{
  "id": "repo-xxx",
  "name": "repo",
  "url": "https://github.com/user/repo.git",
  "branch": "main",
  "localPath": "./data/repos/repo-xxx",
  "status": "COMPLETED",
  "clonedAt": "2024-01-01T00:00:00",
  "lastAnalyzedAt": "2024-01-01T00:00:00"
}
```

---

#### 1.2 获取所有仓库
```
GET /api/repositories
```

**响应 (200 OK)**:
```json
{
  "repo-1": { "id": "repo-1", ... },
  "repo-2": { "id": "repo-2", ... }
}
```

---

#### 1.3 获取单个仓库
```
GET /api/repositories/{id}
```

**响应**:
- `200 OK`: 仓库信息
- `404 Not Found`: 仓库不存在

---

#### 1.4 删除仓库
```
DELETE /api/repositories/{id}
```

**响应**: `204 No Content`

---

### 2. 分析功能 API

#### 2.1 架构分析
```
GET /api/analysis/architecture/{repoId}
```

**响应 (200 OK)**:
```json
{
  "repositoryId": "repo-xxx",
  "overallStructure": "...",
  "mainModules": ["module1", "module2"],
  "moduleDescriptions": { "module1": "..." },
  "designPatterns": ["MVC"],
  "keyFiles": ["pom.xml", "README.md"],
  "techStack": "Java, Spring Boot",
  "recommendations": ["..."]
}
```

---

#### 2.2 源码走读
```
GET /api/analysis/walkthrough/{repoId}?filePath=path/to/file.java
```

**响应 (200 OK)**:
```json
{
  "repositoryId": "repo-xxx",
  "filePath": "path/to/file.java",
  "fileSummary": "...",
  "sections": [
    {
      "sectionName": "File Overview",
      "startLine": 1,
      "endLine": 50,
      "explanation": "...",
      "codeSnippet": "..."
    }
  ],
  "dependencies": ["java.util.List"],
  "dependents": []
}
```

---

#### 2.3 错误堆栈分析
```
POST /api/analysis/error-stack
```

**请求体**:
```json
{
  "repositoryId": "repo-xxx",
  "errorStack": "java.lang.NullPointerException\n\tat com.example.MyClass.method(MyClass.java:42)\n..."
}
```

**响应 (200 OK)**:
```json
{
  "errorType": "java.lang.NullPointerException",
  "rootCause": "...",
  "summary": "...",
  "suspectedLocations": [
    {
      "className": "com.example.MyClass",
      "methodName": "method",
      "filePath": "src/main/java/com/example/MyClass.java",
      "lineNumber": 42,
      "confidence": 0.95,
      "description": "Found in stack trace"
    }
  ],
  "relatedCode": [
    {
      "filePath": "src/main/java/com/example/MyClass.java",
      "codeSnippet": "...",
      "relevance": "High"
    }
  ],
  "possibleFixes": ["Check null values", "..."]
}
```

---

#### 2.4 获取文件列表
```
GET /api/analysis/files/{repoId}
```

**响应 (200 OK)**:
```json
[
  "src/main/java/com/example/MyClass.java",
  "pom.xml",
  "README.md"
]
```

---

#### 2.5 AI 对话
```
POST /api/analysis/chat
```

**请求体**:
```json
{
  "repositoryId": "repo-xxx",
  "message": "分析一下这个项目的架构"
}
```

**响应 (200 OK)**:
```json
{
  "message": "## 项目架构分析...",
  "timestamp": "2024-01-01T00:00:00"
}
```

---

## 扩展开发指南

### 1. 添加新的 LLM 提供商

#### 步骤:
1. 在 `application.yml` 中添加新提供商的配置
2. 创建新的 Service 类（如 `OpenAIService.java`）
3. 在 `ChatService` 中添加条件判断，根据配置选择提供商

**示例**:
```java
@Service
public class ChatService {
    private final ChatProvider chatProvider;

    public ChatService(AnalyzerProperties properties) {
        if (properties.getOpenai() != null) {
            this.chatProvider = new OpenAIChatProvider(properties);
        } else {
            this.chatProvider = new DashScopeChatProvider(properties);
        }
    }

    public String chat(String message) {
        return chatProvider.sendMessage(message);
    }
}
```

---

### 2. 集成向量数据库（替代内存存储）

#### 推荐数据库:
- **ChromaDB** (轻量级, 本地)
- **Milvus** (生产级, 分布式)
- **Weaviate** (云原生)

#### 步骤:
1. 添加依赖到 `pom.xml`
2. 修改 `VectorStoreService`，实现数据库集成
3. 保留原有接口，确保向后兼容

**ChromaDB 集成示例**:
```xml
<dependency>
    <groupId>com.github.alex3d</groupId>
    <artifactId>chroma-java-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

### 3. 添加新的代码分析功能

#### 步骤:
1. 在 `RepositoryAnalysisService` 中添加新方法
2. 创建对应的 DTO 类（在 `model/dto/` 下）
3. 在 `AnalysisController` 中添加端点
4. 在前端创建对应页面

**示例: 代码复杂度分析**
```java
// DTO
public class ComplexityAnalysisResponse {
    private String filePath;
    private int cyclomaticComplexity;
    private int cognitiveComplexity;
    // ...
}

// Service
public ComplexityAnalysisResponse analyzeComplexity(String repoId, String filePath) {
    // 实现复杂度计算
}

// Controller
@GetMapping("/complexity/{repoId}")
public ResponseEntity<ComplexityAnalysisResponse> analyzeComplexity(
        @PathVariable String repoId,
        @RequestParam String filePath
) {
    return ResponseEntity.ok(analysisService.analyzeComplexity(repoId, filePath));
}
```

---

### 4. 添加新的前端页面

#### 步骤:
1. 在 `frontend/src/pages/` 下创建新组件
2. 在 `App.tsx` 中添加路由
3. 在 `Header` 或导航中添加链接
4. 在 `api.ts` 中添加 API 调用方法

**示例**:
```tsx
// frontend/src/pages/NewFeature.tsx
export default function NewFeature() {
  return <div>新功能页面</div>;
}

// frontend/src/App.tsx
<Route path="/new-feature" element={<NewFeature />} />
```

---

## 常见问题

### Q1: 如何修改代码分块大小？
**A**: 修改 `CodeParserService.getChunkSize()` 方法。

### Q2: 向量数据重启后丢失怎么办？
**A**: 当前使用内存存储，需要集成持久化向量数据库（见扩展开发指南）。

### Q3: 如何添加新的编程语言支持？
**A**: 在 `CodeParserService.SUPPORTED_EXTENSIONS` 中添加扩展名，在 `getChunkSize()` 中添加分块策略。

### Q4: 前端如何连接到远程后端？
**A**: 修改 `frontend/src/services/api.ts` 中的 `baseURL`。

### Q5: 如何调试 LLM API 调用？
**A**: 开启 DEBUG 日志，在 `application.yml` 中设置：
```yaml
logging:
  level:
    com.ai.analyzer: DEBUG
```

---

## 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 许可证

MIT License - 详见 LICENSE 文件
