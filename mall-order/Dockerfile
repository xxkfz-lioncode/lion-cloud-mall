# 订单服务镜像（需先在模块目录下执行 mvn package 生成 jar）
FROM eclipse-temurin:21-jre
LABEL maintainer="lion"
WORKDIR /app

ARG JAR_FILE=target/mall-order.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    NACOS_ADDR="nacos:8848" \
    MYSQL_HOST="mysql" \
    MYSQL_PORT="3306" \
    REDIS_HOST="redis" \
    SW_AGENT_NAME="mall-order" \
    SW_AGENT_COLLECTOR_BACKEND_SERVICES="oap:11800"

EXPOSE 8103

# SkyWalking Agent：只有挂载了 /skywalking/agent 才启用，未挂载时不影响正常启动
ENTRYPOINT ["sh", "-c", "AGENT=/skywalking/agent/skywalking-agent.jar; if [ -f \"$AGENT\" ]; then JAVA_OPTS=\"$JAVA_OPTS -javaagent:$AGENT -Dskywalking.agent.service_name=${SW_AGENT_NAME} -Dskywalking.collector.backend_service=${SW_AGENT_COLLECTOR_BACKEND_SERVICES}\"; fi; java ${JAVA_OPTS} -jar /app/app.jar"]
