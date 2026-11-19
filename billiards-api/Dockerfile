FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir -p /app/uploads && chown app /app/uploads
COPY --from=build /app/build/libs/*.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod UPLOAD_DIR=/app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
