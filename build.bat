@echo off
:: NOTE: This file must be plain ASCII (no Chinese / no special symbols).
:: cmd.exe decides how to decode a .bat file when it opens it, using the
:: console code page of the caller. Encoding cannot be switched from inside
:: the script, so any multi-byte character would break on a machine whose
:: default code page differs from the file encoding.
cd /d "%~dp0"
setlocal enabledelayedexpansion
title Lion Mall - Build and Deploy

:: ===================== Environment =====================
SET "JAVA_HOME=C:\Program Files\Java\jdk-21.0.1"
SET "MAVEN_HOME=D:\apache\maven\apache-maven-3.6.3"
SET "PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%"

:: ===================== Main menu =====================
:MENU
cls
echo.
echo ============================================================
echo              Lion Mall  -  Build and Deploy
echo ============================================================
echo   Select the module to build:
echo.
echo      1^)  ALL modules (common + api + all services)
echo      2^)  mall-gateway     Gateway
echo      3^)  mall-user        User service
echo      4^)  mall-product     Product service
echo      5^)  mall-order       Order service
echo      6^)  mall-frontend    Frontend (npm build inside image)
echo      7^)  Public modules only (mall-common + mall-api)
echo.
echo      0^)  Exit
echo ============================================================
set /p OP=Enter number:

:: NEED_MVN=1 means run Maven; empty MVN_MOD means "no -pl", i.e. build all
if "%OP%"=="1" ( set "NAME=ALL modules"    & set "NEED_MVN=1" & set "MVN_MOD="                        & set "SVC="             & goto ASK_DEPLOY )
if "%OP%"=="2" ( set "NAME=mall-gateway"   & set "NEED_MVN=1" & set "MVN_MOD=-pl mall-gateway -am"    & set "SVC=mall-gateway"  & goto ASK_DEPLOY )
if "%OP%"=="3" ( set "NAME=mall-user"      & set "NEED_MVN=1" & set "MVN_MOD=-pl mall-user -am"       & set "SVC=mall-user"     & goto ASK_DEPLOY )
if "%OP%"=="4" ( set "NAME=mall-product"   & set "NEED_MVN=1" & set "MVN_MOD=-pl mall-product -am"    & set "SVC=mall-product"  & goto ASK_DEPLOY )
if "%OP%"=="5" ( set "NAME=mall-order"     & set "NEED_MVN=1" & set "MVN_MOD=-pl mall-order -am"      & set "SVC=mall-order"    & goto ASK_DEPLOY )
if "%OP%"=="6" ( set "NAME=mall-frontend"  & set "NEED_MVN=0" & set "MVN_MOD="                        & set "SVC=mall-frontend" & goto ASK_DEPLOY )
:: Public modules have no container, so just build and skip the Docker prompt
if "%OP%"=="7" ( set "NAME=public modules" & set "NEED_MVN=1" & set "MVN_MOD=-pl mall-common,mall-api" & set "SVC=" & set "DO_DEPLOY=0" & goto DO_BUILD )
if "%OP%"=="0" exit /b 0

echo [WARN] Invalid input, try again
timeout /t 1 >nul
goto MENU

:: ===================== Deploy prompt =====================
:ASK_DEPLOY
cls
echo.
echo ============================================================
echo   Selected: %NAME%
echo ============================================================
echo   Deploy to Docker after building?
echo.
echo      1^)  Yes  -  rebuild image and start container
echo              docker compose up -d --build %SVC%
echo      2^)  No   -  build only, leave containers untouched
echo.
echo      0^)  Back
echo ============================================================
set /p DEP=Enter number:

if "%DEP%"=="1" ( set "DO_DEPLOY=1" & goto DO_BUILD )
if "%DEP%"=="2" ( set "DO_DEPLOY=0" & goto DO_BUILD )
if "%DEP%"=="0" goto MENU

echo [WARN] Invalid input, try again
timeout /t 1 >nul
goto ASK_DEPLOY

:: ===================== Run =====================
:DO_BUILD
cls
echo.
echo ============================================================
echo   Processing: %NAME%
echo ============================================================
echo   JAVA_HOME = %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version 2>&1
echo.

:: -DskipTests : skip tests to speed things up
:: -am (also make) : also build dependency modules, e.g. choosing
::                   mall-order also builds mall-common and mall-api
if not "%NEED_MVN%"=="0" (
    echo [1/2] Running Maven build ...
    call mvn -DskipTests clean package %MVN_MOD%
    if errorlevel 1 goto FAIL
    echo.
)

if "%DO_DEPLOY%"=="1" (
    if "%SVC%"=="" (
        echo [2/2] Rebuilding and starting all containers ...
        docker compose up -d --build
    ) else (
        echo [2/2] Rebuilding and starting container: %SVC% ...
        docker compose up -d --build %SVC%
    )
    if errorlevel 1 goto FAIL
    echo.
)

echo ============================================================
echo   [SUCCESS] %NAME% done
echo ============================================================
echo   1^)  Back to menu
echo   0^)  Exit
set /p AGAIN=Enter number:
if "%AGAIN%"=="1" goto MENU
exit /b 0

:: ===================== Failure =====================
:FAIL
echo.
echo ============================================================
echo   [FAILED] %NAME% failed - check the log above
echo ============================================================
pause
goto MENU
