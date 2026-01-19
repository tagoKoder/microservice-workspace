# syntax=docker/dockerfile:1

########################################
# Build stage
########################################
FROM golang:1.24.4 AS build
WORKDIR /src

# Cache de dependencias
COPY go.mod go.sum ./
RUN go mod download

# Copia el resto del código
COPY . .

# Build (binario linux)
# -trimpath: reduce paths
# -ldflags "-s -w": reduce tamaño
# CGO_ENABLED=0: binario sin libc (mejor para distroless)
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
  go build -trimpath -ldflags="-s -w" -o /out/ledger ./cmd/ledger

########################################
# Runtime stage (distroless)
########################################
FROM gcr.io/distroless/static-debian12:nonroot AS runtime
WORKDIR /app

COPY --from=build /out/ledger /app/ledger

# gRPC port típico: ajusta al que uses (ej: 9093)
EXPOSE 9093

# Distroless ya corre como nonroot
ENTRYPOINT ["/app/ledger"]
