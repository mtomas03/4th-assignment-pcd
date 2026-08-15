# Build stage: compile the Scala sources and assemble a single fat jar
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.11_10_1.12.15_3.3.8 AS builder

WORKDIR /build
COPY . .
RUN sbt assembly

# Runtime stage: only the JRE and the produced jar are shipped
FROM eclipse-temurin:21-jre

WORKDIR /alarm-system
COPY --from=builder /build/target/app.jar ./app.jar

# The role is passed as the container command
ENTRYPOINT ["java", "-jar", "app.jar"]
