package config

import (
	"log"

	"github.com/joho/godotenv"
)

type CommonConfig struct {
	OtelExporterOtlpEndpoint string
	ServiceName              string
	Environment              string
	Version                  string
	LogLevel                 string
	LogFilePath              string
	SentryDSN                string
	SentryEnv                string
	SentryRelease            string
	// Baggage / Sentry / Log filters
	BaggagePrefix       string
	BaggageAllow        map[string]struct{}
	BaggageDeny         map[string]struct{}
	BaggageMaxKeys      int
	BaggageMaxVal       int
	BaggageUserIDKey    string
	BaggageUserEmailKey string
}

// LoadConfig loads environment variables from the .env file and returns a Config struct
func LoadConfig() (*CommonConfig, error) {
	// Load the .env file (if it exists)
	err := godotenv.Load()
	if err != nil {
		log.Println("Error loading .env file:", err)
	}

	config := &CommonConfig{
		OtelExporterOtlpEndpoint: GetEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "otel-collector:4317"),
		ServiceName:              GetEnv("SERVICE_NAME", ""),
		Environment:              GetEnv("ENVIRONMENT", ""),
		Version:                  GetEnv("VERSION", ""),
		LogLevel:                 GetEnv("LOG_LEVEL", ""),
		LogFilePath:              GetEnv("LOG_FILE_PATH", ""),
		SentryDSN:                GetEnv("SENTRY_DSN", ""),
		SentryEnv:                GetEnv("SENTRY_ENV", "dev"),
		SentryRelease:            GetEnv("SENTRY_RELEASE", ""),
		// Baggage & Sentry/Logger filters
		BaggagePrefix:       GetEnv("BAGGAGE_PREFIX", "custom."),
		BaggageAllow:        GetEnvSet("BAGGAGE_ALLOW", []string{"custom.user_id", "custom.user_email", "custom.tenant_id"}), // default útil
		BaggageDeny:         GetEnvSet("BAGGAGE_DENY", nil),
		BaggageMaxKeys:      GetEnvInt("BAGGAGE_MAX_KEYS", 16),
		BaggageMaxVal:       GetEnvInt("BAGGAGE_MAX_VAL", 256),
		BaggageUserIDKey:    GetEnv("BAGGAGE_USER_ID_KEY", "custom.user_id"),
		BaggageUserEmailKey: GetEnv("BAGGAGE_USER_EMAIL_KEY", "custom.user_email"),
	}
	return config, nil
}
