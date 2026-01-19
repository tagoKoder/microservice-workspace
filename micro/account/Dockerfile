# syntax=docker/dockerfile:1

########################################
# Build stage
########################################
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copiamos primero metadata para aprovechar cache
COPY pom.xml .
COPY src ./src

# Si usas protos dentro de src/main/proto, ya están en src.
# Si tu build depende de "contracts/sync-protos.ps1", en Linux/Docker no existirá Powershell.
# Recomendación: deshabilita ese exec-maven-plugin en Docker via profile (ver nota abajo).
RUN ./mvnw -v 2>/dev/null || true
RUN mvn -q -DskipTests package

########################################
# Runtime stage
########################################
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Usuario no-root
RUN useradd -r -u 10001 appuser
USER 10001

# Copia el jar
COPY --from=build /workspace/target/account-0.0.1-SNAPSHOT.jar /app/app.jar

# gRPC server port (según tu GrpcServerConfig: 9091 por default)
EXPOSE 9091

# Recomendado para contenedor
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

# Spring profile opcional (si quieres)
# ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java","-jar","/app/app.jar"]
