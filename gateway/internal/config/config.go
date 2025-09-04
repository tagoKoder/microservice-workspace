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
	OidcIssuers         []string
	OidcAudience        string
	OidcAudiencesMap    map[string]string
	WhiteListOrigin     []string
	HeaderIDTokenName   string
	AuthentikApiKey     string
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
		OidcIssuers:         commonCfg.GetEnvList("OIDC_ISSUERS", nil),
		OidcAudience:        commonCfg.GetEnv("OIDC_AUDIENCE", ""),
		OidcAudiencesMap:    commonCfg.GetEnvMap("OIDC_AUDIENCES_MAP", nil),
		WhiteListOrigin:     commonCfg.GetEnvList("WHITE_LIST_ORIGIN", nil),
		HeaderIDTokenName:   commonCfg.GetEnv("HEADER_ID_TOKEN_NAME", "X-ID-Token"),
		AuthentikApiKey:     commonCfg.GetEnv("AUTHENTIK_API_KEY", ""),
		CommonConfig:        common,
	}
	return config, nil
}
