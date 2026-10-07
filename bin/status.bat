@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   容器状态
echo ============================================
docker compose ps
pause
