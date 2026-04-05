# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Build
# Uses the full JDK to compile and package the application.
# The build stage is discarded after packaging — it never reaches production.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy dependency manifests first — Docker layer cache means this layer
# is only re-run when pom.xml changes, not on every source code change.
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Download dependencies into the Docker cache layer
RUN ./mvnw dependency:go-offline -q

# Now copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# Extract layered jar for optimal Docker caching on subsequent builds
# (Spring Boot layered jars split: dependencies / spring-boot-loader / snapshot-dependencies / application)
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime
# Uses a minimal JRE-only image — no compiler, no build tools, smaller attack surface.
# eclipse-temurin:21-jre-alpine is ~90MB vs ~350MB for the full JDK.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copy layered jar contents in dependency-stable → volatile order
# so unchanged layers (e.g. Spring framework jars) are reused across builds
COPY --from=build /workspace/target/extracted/dependencies/          ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/    ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/           ./

EXPOSE 8080

# JVM tuning flags for containerised environments:
#   -XX:+UseContainerSupport        — respects Docker memory limits (not host RAM)
#   -XX:MaxRAMPercentage=75.0       — use up to 75% of container RAM for heap
#   -XX:+UseG1GC                    — G1 garbage collector, good balance for APIs
#   -Djava.security.egd=...         — faster SecureRandom init (avoids /dev/random blocking)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
