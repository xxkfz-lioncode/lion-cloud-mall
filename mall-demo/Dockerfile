# 演示服务镜像（需先在模块目录下执行 mvn package 生成 jar）
# 与业务服务镜像的区别：不挂载 SkyWalking Agent，保持最简，方便排查和学习
FROM eclipse-temurin:21-jre
LABEL maintainer="lion"
WORKDIR /app

ARG JAR_FILE=target/mall-demo.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m" \
    SERVER_PORT="8100" \
    DEMO_MESSAGE="Hello from mall-demo"

EXPOSE 8100

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
