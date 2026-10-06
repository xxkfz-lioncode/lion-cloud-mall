@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   一键启动：打包 + 全栈启动
echo ============================================
echo [1/2] 打包...
call mvn -DskipTests clean package
if errorlevel 1 (
    echo.
    echo [失败] 打包出错，已中止
    pause
    exit /b 1
)

echo.
echo [2/2] 构建镜像并启动容器...
docker compose up -d --build

echo.
docker compose ps

echo.
echo 前端页面：      http://localhost
echo 网关：          http://localhost:8080
echo Nacos：         http://localhost:8848/nacos   （nacos / nacos）
echo SkyWalking：    http://localhost:8081
pause
