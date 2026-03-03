#!/bin/bash

echo "🤖 AI Source Code Analyzer - 快速启动"
echo "===================================="

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! command -v docker compose &> /dev/null; then
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

# 检查 Java 是否安装
if ! command -v java &> /dev/null; then
    echo "❌ Java 17+ 未安装，请先安装 Java 17 或更高版本"
    exit 1
fi

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装，请先安装 Maven"
    exit 1
fi

# 检查 Node.js 是否安装
if ! command -v node &> /dev/null; then
    echo "❌ Node.js 未安装，请先安装 Node.js 18 或更高版本"
    exit 1
fi

echo "✅ 环境检查通过"
echo ""

# 启动 ChromaDB
echo "📦 启动 ChromaDB 向量数据库..."
if command -v docker-compose &> /dev/null; then
    docker-compose up -d
else
    docker compose up -d
fi

# 等待 ChromaDB 启动
echo "⏳ 等待 ChromaDB 启动..."
sleep 10

echo ""
echo "🚀 启动后端服务..."
cd backend
mvn spring-boot:run &
BACKEND_PID=$!
cd ..

echo ""
echo "🌐 启动前端服务..."
cd frontend
npm install
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "✨ 服务启动完成！"
echo ""
echo "📖 访问地址："
echo "   前端: http://localhost:3000"
echo "   后端: http://localhost:8080"
echo "   ChromaDB: http://localhost:8000"
echo ""
echo "按 Ctrl+C 停止所有服务"

# 等待用户中断
wait $BACKEND_PID $FRONTEND_PID
