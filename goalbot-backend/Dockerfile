FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache tzdata

ENV TZ=Asia/Shanghai
WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/goalbot-backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/goalbot-backend.jar"]
