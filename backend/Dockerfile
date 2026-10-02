# ---- 第一階段：用 Maven 打包 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先只複製 pom.xml 並下載依賴：只改程式碼時可重用這層快取，不必每次重新下載所有 jar
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- 第二階段：只帶 JRE 與 jar 執行 ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 以非 root 使用者執行，降低容器被入侵時的權限
RUN addgroup -S splitmate && adduser -S splitmate -G splitmate
USER splitmate
COPY --from=build /app/target/splitmate-backend-*.jar app.jar
EXPOSE 8080
# MaxRAMPercentage：依容器記憶體上限配置 heap，避免小台 VPS 上 JVM 吃光記憶體被 OOM kill
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
