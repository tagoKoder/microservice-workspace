# syntax=docker/dockerfile:1.6

########################################
# Build stage
########################################
FROM golang:1.24.4-alpine AS build

# Por si algún módulo requiere git o certs
RUN apk add --no-cache git ca-certificates

WORKDIR /src

# Cache de dependencias (mejor con BuildKit cache)
COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    go mod download

# Copia el resto del código
COPY . .

# Build (usa TARGETOS/TARGETARCH para cross-build limpio)
ARG TARGETOS=linux
ARG TARGETARCH=amd64

RUN --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=$TARGETOS GOARCH=$TARGETARCH \
    go build -trimpath -ldflags="-s -w" -o /out/ledger ./cmd/ledger

########################################
# Runtime stage (distroless)
########################################
FROM gcr.io/distroless/static-debian12:nonroot AS runtime
WORKDIR /app

COPY --from=build /out/ledger /app/ledger

EXPOSE 9093
ENTRYPOINT ["/app/ledger"]
