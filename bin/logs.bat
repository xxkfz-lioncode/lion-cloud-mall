@echo off
chcp 65001 >nul
cd /d %~dp0..

echo 可选服务：mall-gateway mall-user mall-product mall-order mall-frontend mysql redis nacos skywalking-oap skywalking-ui
set /p SVC=请输入服务名（直接回车默认 mall-gateway）：
if "%SVC%"=="" set SVC=mall-gateway

echo.
echo 实时查看 %SVC% 日志，Ctrl+C 退出
docker compose logs -f --tail=200 %SVC%
pause
