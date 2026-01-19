# syntax=docker/dockerfile:1

########################################
# Build stage
########################################
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# 1) Cache friendly: primero pom
COPY pom.xml .

# 2) Copia fuentes
COPY src ./src

# IMPORTANTE:
# Si tu exec-maven-plugin (powershell sync-protos) se ejecuta en Docker, fallará.
# Recomendación: deshabilitarlo por profile en Docker (ver sección D).
# Aquí asumo que ya está deshabilitado o no corre.
RUN mvn -q -DskipTests package

########################################
# Runtime stage
########################################
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN useradd -r -u 10001 appuser
USER 10001

COPY --from=build /workspace/target/identity-0.0.1-SNAPSHOT.jar /app/app.jar

# Tu gRPC server típicamente 9090/9092/9091. Si usas 9090 por default, cámbialo.
EXPOSE 9090

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java","-jar","/app/app.jar"]
