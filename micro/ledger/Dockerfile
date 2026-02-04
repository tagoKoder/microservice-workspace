# syntax=docker/dockerfile:1.6

FROM golang:1.24.4-alpine AS build
RUN apk add --no-cache git ca-certificates curl

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

# baja el probe en build stage (con curl)
RUN curl -fsSL \
  -o /out/grpc_health_probe \
  https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/v0.4.28/grpc_health_probe-linux-amd64 \
  && chmod +x /out/grpc_health_probe

FROM gcr.io/distroless/static-debian12:nonroot AS runtime
WORKDIR /app
COPY --from=build /out/ledger /app/ledger
COPY --from=build /out/grpc_health_probe /usr/local/bin/grpc_health_probe

EXPOSE 9093
ENTRYPOINT ["/app/ledger"]
