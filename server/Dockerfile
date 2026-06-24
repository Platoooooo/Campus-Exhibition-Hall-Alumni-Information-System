# ── Stage 1: Maven 构建 ──
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
# 先复制 pom.xml 利用 Docker 层缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B -q || true
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: JRE 运行 ──
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
