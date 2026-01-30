FROM golang:1.24-alpine AS build

RUN apk add --no-cache git ca-certificates

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 go build -o /bff ./cmd/bff

FROM alpine:3.20
RUN adduser -D app
USER app
COPY --from=build /bff /bff
EXPOSE 8080
ENTRYPOINT ["/bff"]
