# What: STAGE 1. Use a Java 21 JDK image for the build stage and name it "builder".
# Why: Building requires a full JDK; naming the stage lets us copy only the built jar later.
FROM eclipse-temurin:21-jdk AS builder

# What: Set /app as the working directory inside the build stage.
# Why: Keeps paths predictable for copy and build commands.
WORKDIR /app

# What: Copy Maven Wrapper metadata into the image.
# Why: mvnw needs this metadata to resolve the project-pinned Maven distribution.
COPY .mvn ./.mvn

# What: Copy the Maven Wrapper script.
# Why: Ensures Docker build uses the same Maven version as local/CI environments.
COPY mvnw ./mvnw

# What: Copy the Maven project descriptor.
# Why: Required for dependency/plugin resolution and lifecycle execution.
COPY pom.xml .

# What: Copy the application source code and resources.
# Why: These inputs are compiled and packaged into the runnable Spring Boot jar.
COPY src ./src

# What: Make mvnw executable, then run a full package build including unit tests.
# Why: Ensures container builds fail fast if backend unit tests fail.
RUN chmod +x ./mvnw && ./mvnw clean package

# What: STAGE 2. Start the runtime stage with a Java 21 JRE image.
# Why: JRE-only runtime is smaller and better suited for production than a full JDK image.
FROM eclipse-temurin:21-jre

# What: Set /app as the runtime working directory.
# Why: Keeps artifact location and startup command paths consistent.
WORKDIR /app

# What: Copy the built jar from the builder stage into the runtime image.
# Why: Final image contains only the runtime artifact, not source/build tooling.
COPY --from=builder /app/target/*.jar app.jar

# What: Document that the containerized app listens on port 8080.
# Why: Spring Boot defaults to 8080 and this helps tooling/operators.
EXPOSE 8080

# What: Define the container startup command.
# Why: Runs the packaged Spring Boot application directly via java -jar.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]