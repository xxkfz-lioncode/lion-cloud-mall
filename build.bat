@echo off
chcp 65001 >nul
cls
echo ==================================================
echo   开始执行 Maven 打包，自定义 JDK 路径...
echo ==================================================

:: 1. 指定自定义的 JAVA 环境 (双引号非常重要，因为路径中有空格)
SET "JAVA_HOME=C:\Program Files\Java\jdk-21.0.1"

:: 2. 确保系统能立刻找到 mvn 命令 (如果你之前配置过环境变量可以省略下面两行，加上更保险)
:: 如果你的 Maven 在 D:\maven\apache-maven-3.9.9，请替换下面的路径
SET "MAVEN_HOME=D:\apache\maven\apache-maven-3.6.3"
SET "PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%"

:: 3. 打印当前正在使用的 Java 版本，以防路径搞错
echo 正在使用的 Java 版本信息：
"%JAVA_HOME%\bin\java.exe" -version
echo.

:: 4. 执行 mvn clean install
echo [正在打包] 正在执行 mvn clean install，请耐心等待...
mvn clean install

:: 5. 判断执行结果并反馈
IF %ERRORLEVEL% EQU 0 (
    echo.
    echo ==================================================
    echo   [成功] 恭喜！打包顺利完成。
    echo ==================================================
) ELSE (
    echo.
    echo ==================================================
    echo   [失败] 哎呀，打包过程中出错了，请检查上面的错误日志。
    echo ==================================================
)

pause