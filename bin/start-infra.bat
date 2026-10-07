@echo off
chcp 65001 >nul
cd /d %~dp0..

echo ============================================
echo   启动基础设施：MySQL + Redis + Nacos + SkyWalking + Seata
echo   （只起中间件，业务服务请在 IDEA 里启动）
echo ============================================
docker compose up -d mysql redis nacos skywalking-oap skywalking-ui seata-server

echo.
docker compose ps

echo.
echo Nacos 控制台：http://localhost:8848/nacos   账号 nacos / nacos
echo MySQL：127.0.0.1:3307   root / root
echo Redis：127.0.0.1:6379   密码 lion123
echo SkyWalking UI：http://localhost:8081
echo Seata 控制台：http://localhost:7091   账号 seata / seata
pause
