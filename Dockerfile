FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 devsquad
WORKDIR /app
COPY --from=build --chown=devsquad:devsquad /workspace/build/libs/devsquad-backend-*.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
