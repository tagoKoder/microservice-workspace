# syntax=docker/dockerfile:1.6

########################################
# Build stage
########################################
FROM golang:1.24.4-alpine AS build
RUN apk add --no-cache git ca-certificates
WORKDIR /src

COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    go mod download

COPY . .

ARG TARGETOS=linux
ARG TARGETARCH=amd64
RUN --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=$TARGETOS GOARCH=$TARGETARCH \
    go build -trimpath -ldflags="-s -w" -o /out/ledger ./cmd/ledger

# (Opcional pero útil) Crear passwd/group mínimos para que algunas libs resuelvan home/usuario
RUN printf 'appuser:x:10001:10001:appuser:/home/appuser:/sbin/nologin\n' > /tmp/passwd && \
    printf 'appuser:x:10001:\n' > /tmp/group

########################################
# Runtime stage (distroless)
########################################
FROM gcr.io/distroless/static-debian12:nonroot AS runtime
WORKDIR /app
COPY --from=build /out/ledger /app/ledger

ENV HOME=/home/nonroot
ENV AWS_SDK_LOAD_CONFIG=true
ENV AWS_EC2_METADATA_DISABLED=true
# Opcional determinista:
ENV AWS_CONFIG_FILE=/home/nonroot/.aws/config
ENV AWS_SHARED_CREDENTIALS_FILE=/home/nonroot/.aws/credentials

EXPOSE 9093
ENTRYPOINT ["/app/ledger"]
