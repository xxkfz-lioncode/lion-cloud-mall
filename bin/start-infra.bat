@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   启动基础设施：MySQL + Redis + Nacos
echo ============================================
docker compose -f docker-compose-infra.yml up -d

echo.
docker compose -f docker-compose-infra.yml ps

echo.
echo Nacos 控制台：http://localhost:8848/nacos   账号 nacos / nacos
echo MySQL：127.0.0.1:3307   root / root
echo Redis：127.0.0.1:6379   密码 lion123
pause
