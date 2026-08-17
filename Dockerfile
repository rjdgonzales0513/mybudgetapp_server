###################################################
# Stage: base
# 
# This base stage ensures all other stages are using the same base image
# and provides common configuration for all stages, such as the working dir.
###################################################
# FROM openjdk:25-ea-21-jdk-slim

# # 2. Set working directory
# WORKDIR /app

# # 3. Copy files
# COPY /target/demo-0.0.1-SNAPSHOT.jar app.jar

# # 4. Command to run
# CMD ["java", "-jar", "app.jar"]

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package

# Runtime stage
FROM openjdk:21-ea-21-jdk-slim
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]