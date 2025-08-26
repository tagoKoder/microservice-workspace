package config

import (
	"log"

	"github.com/joho/godotenv"
	commonCfg "github.com/tagoKoder/common-kit/config"
)

type Config struct {
	// NOT SHARED
	DbDNS    string
	GrpcPort string
	*commonCfg.CommonConfig
}

// LoadConfig loads environment variables from the .env file and returns a Config struct
func LoadConfig() (*Config, error) {
	// Load the .env file (if it exists)
	err := godotenv.Load()
	if err != nil {
		log.Println("Error loading .env file:", err)
	}
	common, err := commonCfg.LoadConfig()

	if err != nil {
		log.Println("Error loading common config:", err)
	}

	config := &Config{
		GrpcPort:     commonCfg.GetEnv("GRPC_PORT", ""),
		DbDNS:        commonCfg.GetEnv("DB_DNS", ""),
		CommonConfig: common,
	}
	return config, nil
}
