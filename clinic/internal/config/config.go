package config

import (
	"log"
	"os"

	"github.com/joho/godotenv"
)

type Config struct {
	OtelExporterOtlpEndpoint string
	ServiceName              string
	Environment              string
	Version                  string
	GrpcPort                 string
	DbDNS                    string
	LogLevel                 string
	LogFilePath              string
	SentryDNS                string
	SentryEnv                string
	SentryRelease            string
}

// LoadConfig loads environment variables from the .env file and returns a Config struct
func LoadConfig() (*Config, error) {
	// Load the .env file (if it exists)
	err := godotenv.Load()
	if err != nil {
		log.Println("Error loading .env file:", err)
	}

	config := &Config{
		OtelExporterOtlpEndpoint: GetEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "otel-collector:4317"),
		ServiceName:              GetEnv("SERVICE_NAME", ""),
		Environment:              GetEnv("ENVIRONMENT", ""),
		Version:                  GetEnv("VERSION", ""),
		GrpcPort:                 GetEnv("GRPC_PORT", ""),
		DbDNS:                    GetEnv("DB_DNS", ""),
		LogLevel:                 GetEnv("LOG_LEVEL", ""),
		LogFilePath:              GetEnv("LOG_FILE_PATH", ""),
		SentryDNS:                GetEnv("SENTRY_DSN", ""),
		SentryEnv:                GetEnv("SENTRY_ENV", "dev"),
		SentryRelease:            GetEnv("SENTRY_RELEASE", ""),
	}
	return config, nil
}

// GetEnv gets an environment variable or returns a default value
func GetEnv(key string, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}
