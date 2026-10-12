# Build the application using the project's Gradle Wrapper.
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# Handle Windows line endings and executable permissions.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src ./src

# Package the application. Tests run separately with test infrastructure.
RUN ./gradlew --no-daemon bootJar \
    && mkdir /artifact \
    && find build/libs -maxdepth 1 -name '*.jar' \
       ! -name '*-plain.jar' -exec cp {} /artifact/app.jar \;

# Run with a smaller Java runtime, without Gradle or source code.
FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --create-home app

WORKDIR /app

COPY --from=builder --chown=app:app /artifact/app.jar ./app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]