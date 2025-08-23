package config

import (
	"log"

	"github.com/joho/godotenv"
	commonCfg "github.com/tagoKoder/common-kit/config"
)

type Config struct {
	HttpPort            string
	IdentityServiceAddr string
	ClinicServiceAddr   string
	OidcIssuer          string
	WhiteListOrigin     []string
	HeaderIDTokenName   string
	*commonCfg.CommonConfig
}

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
		HttpPort:            commonCfg.GetEnv("HTTP_PORT", ""),
		IdentityServiceAddr: commonCfg.GetEnv("IDENTITY_SERVICE_ADDR", ""),
		ClinicServiceAddr:   commonCfg.GetEnv("CLINIC_SERVICE_ADDR", ""),
		OidcIssuer:          commonCfg.GetEnv("OIDC_ISSUER", ""),
		WhiteListOrigin:     commonCfg.GetEnvList("WHITE_LIST_ORIGIN", nil),
		HeaderIDTokenName:   commonCfg.GetEnv("HEADER_ID_TOKEN_NAME", "X-ID-Token"),
		CommonConfig:        common,
	}
	return config, nil
}
