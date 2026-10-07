@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   重置：删除容器并清空所有数据卷
echo   （MySQL / Redis / Nacos / SkyWalking 数据全部丢失）
echo ============================================
set /p OK=确认执行？输入 y 继续，其它任意键取消：
if /i not "%OK%"=="y" (
    echo 已取消
    pause
    exit /b 0
)

docker compose down -v

echo.
echo 已重置。重新执行 start-all.bat 即可（MySQL 会重新执行建表脚本）
pause
