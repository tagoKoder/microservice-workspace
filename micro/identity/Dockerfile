# syntax=docker/dockerfile:1

########################################
# Build stage (con Maven incluido)
########################################
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# cache-friendly
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
# Desactiva plugins que dependan de PowerShell o SO si aplica
RUN mvn -q -DskipTests -DskipProtos=true package

########################################
# Runtime stage
########################################
FROM eclipse-temurin:25-jre
WORKDIR /app

# Crea usuario no root (Ubuntu/UBI: usar 'useradd' requiere root)
RUN useradd -r -u 10001 appuser
USER 10001

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 9090
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["java","-jar","/app/app.jar"]