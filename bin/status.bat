@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   容器状态
echo ============================================
echo ----- 全栈（docker-compose.yml）-----
docker compose ps

echo.
echo ----- 基础设施（docker-compose-infra.yml）-----
docker compose -f docker-compose-infra.yml ps
pause
