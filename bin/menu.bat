@echo off
chcp 65001 >nul
cd /d %~dp0..
title lion-cloud-mall 运维菜单

:menu
cls
echo ============================================
echo            lion-cloud-mall 运维菜单
echo ============================================
echo   1  一键启动（打包 + 全栈启动）
echo   2  只启动基础设施（MySQL / Redis / Nacos）
echo   3  打包
echo   4  查看容器状态
echo   5  查看实时日志
echo   6  停止
echo   7  重置（清空所有数据）
echo   8  下载 SkyWalking Agent
echo   0  退出
echo ============================================
set /p OP=请输入序号：

if "%OP%"=="1" call "%~dp0start-all.bat"
if "%OP%"=="2" call "%~dp0start-infra.bat"
if "%OP%"=="3" call "%~dp0build.bat"
if "%OP%"=="4" call "%~dp0status.bat"
if "%OP%"=="5" call "%~dp0logs.bat"
if "%OP%"=="6" call "%~dp0stop.bat"
if "%OP%"=="7" call "%~dp0reset.bat"
if "%OP%"=="8" call "%~dp0download-agent.bat"
if "%OP%"=="0" exit /b 0

goto :menu
