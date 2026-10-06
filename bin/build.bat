@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   打包所有模块（跳过测试）
echo ============================================
call mvn -DskipTests clean package

if errorlevel 1 (
    echo.
    echo [失败] 打包出错，请看上面的 Maven 日志
) else (
    echo.
    echo [成功] 各模块 jar 已生成在 target\ 目录
)
pause
