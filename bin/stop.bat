@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   停止所有容器（保留数据与镜像）
echo ============================================
docker compose stop

echo.
echo 已停止。想清空数据请用 reset.bat，想重新启动用 start-all.bat / start-infra.bat
pause
