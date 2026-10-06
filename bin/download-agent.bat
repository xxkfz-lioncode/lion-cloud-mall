@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   下载 SkyWalking Java Agent 并清理残留文件
echo ============================================
powershell -ExecutionPolicy Bypass -File .\docker\skywalking\download-agent.ps1

echo.
echo 清理官方压缩包里的 macOS 残留文件（._*），否则启动时会报 zip END header not found
powershell -NoProfile -Command "Get-ChildItem -Path '%~dp0..\docker\skywalking\agent' -Recurse -Force -Filter '._*' | Remove-Item -Force"

echo.
echo 完成
pause
