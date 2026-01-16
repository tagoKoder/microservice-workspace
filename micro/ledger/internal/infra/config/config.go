package config

import (
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	AppEnv   string
	GrpcAddr string
	ServiceName string

	// AWS
	AWSRegion string

	// JWT (Cognito)
	JWTIssuer   string
	JWTAudience string
	JWTJWKSURL  string

	// AVP
	AVPPolicyStoreID string

	// Audit EventBridge
	AuditEventBusName string
	AuditSource       string
	AuditDetailType   string

	// Hash salts (no PII cruda)
	HashSaltIP string
	HashSaltUA string
	// DB
	DBReadDSN  string
	DBWriteDSN string

	// Accounts (REST internal)
	AccountsBaseURL     string
	AccountsInternalTok string

	// Kafka
	KafkaBrokers  []string
	KafkaClientID string

	// Audit
	AuditTopic string

	// Outbox worker
	OutboxBatchSize    int
	OutboxPollInterval time.Duration

	// Security (opcional)
	InternalTokenHeader string
}

func Load() Config {
	return Config{
		AppEnv:   getEnv("APP_ENV", "dev"),
		GrpcAddr: getEnv("GRPC_ADDR", ":8082"),
		ServiceName: getEnv("SERVICE_NAME", "ledger-payments"),

		AWSRegion: getEnv("AWS_REGION", "us-east-1"),

		JWTIssuer:   getEnv("JWT_ISSUER", ""),
		JWTAudience: getEnv("JWT_AUDIENCE", ""),
		JWTJWKSURL:  getEnv("JWT_JWKS_URL", ""),

		AVPPolicyStoreID: getEnv("AVP_POLICY_STORE_ID", ""),

		AuditEventBusName: getEnv("AUDIT_EVENTBUS_NAME", "default"),
		AuditSource:       getEnv("AUDIT_SOURCE", "banking.ledger"),
		AuditDetailType:   getEnv("AUDIT_DETAIL_TYPE", "AuditEvent"),

		HashSaltIP: getEnv("HASH_SALT_IP", "ip_salt"),
		HashSaltUA: getEnv("HASH_SALT_UA", "ua_salt"),
		DBReadDSN:  getEnv("DB_READ_DSN", getEnv("DB_DSN", "")),
		DBWriteDSN: getEnv("DB_WRITE_DSN", getEnv("DB_DSN", "")),

		AccountsBaseURL:     strings.TrimRight(getEnv("ACCOUNTS_BASE_URL", "http://accounts:8080"), "/"),
		AccountsInternalTok: getEnv("ACCOUNTS_INTERNAL_TOKEN", ""),

		KafkaBrokers:  splitCSV(getEnv("KAFKA_BROKERS", "localhost:9092")),
		KafkaClientID: getEnv("KAFKA_CLIENT_ID", "ledger-payments"),

		AuditTopic: getEnv("AUDIT_TOPIC", "audit.events"),

		OutboxBatchSize:    getEnvInt("OUTBOX_BATCH_SIZE", 50),
		OutboxPollInterval: getEnvDuration("OUTBOX_POLL_INTERVAL", 200*time.Millisecond),

		InternalTokenHeader: getEnv("INTERNAL_TOKEN_HEADER", "X-Internal-Token"),
	}
}

func getEnv(k, def string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return def
}

func getEnvInt(k string, def int) int {
	v := os.Getenv(k)
	if v == "" {
		return def
	}
	i, err := strconv.Atoi(v)
	if err != nil {
		return def
	}
	return i
}

func getEnvDuration(k string, def time.Duration) time.Duration {
	v := os.Getenv(k)
	if v == "" {
		return def
	}
	d, err := time.ParseDuration(v)
	if err != nil {
		return def
	}
	return d
}

func splitCSV(s string) []string {
	parts := strings.Split(s, ",")
	var out []string
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	return out
}
