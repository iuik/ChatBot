FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml ./
COPY .mvn .mvn
COPY src src
COPY prompts prompts

RUN mvn -s .mvn/settings.xml -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Shanghai

COPY --from=build /build/target/chatbot-0.0.1-SNAPSHOT.jar /app/chatbot.jar
COPY --from=build /build/prompts /app/prompts

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "/app/chatbot.jar"]
