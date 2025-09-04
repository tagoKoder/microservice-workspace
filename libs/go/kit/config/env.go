package config

import (
	"encoding/json"
	"log"
	"os"
	"strconv"
	"strings"
)

// GetEnv gets a string env var with default.
func GetEnv(key, def string) string {
	if v, ok := os.LookupEnv(key); ok {
		return v
	}
	return def
}

// GetEnvInt gets an int env var with default; logs a warning if malformed.
func GetEnvInt(key string, def int) int {
	if v, ok := os.LookupEnv(key); ok {
		v = strings.TrimSpace(v)
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
		log.Printf("WARN: %s has invalid int %q, using default %d", key, v, def)
	}
	return def
}

// GetEnvBool gets a bool env var with default (true/false/1/0/yes/no).
func GetEnvBool(key string, def bool) bool {
	if v, ok := os.LookupEnv(key); ok {
		switch strings.ToLower(strings.TrimSpace(v)) {
		case "1", "true", "yes", "y":
			return true
		case "0", "false", "no", "n":
			return false
		default:
			log.Printf("WARN: %s has invalid bool %q, using default %v", key, v, def)
		}
	}
	return def
}

// GetEnvList reads a list from env: accepts JSON array (["a","b"]) or CSV ("a,b").
// Trims whitespace, drops empty entries.
func GetEnvList(key string, def []string) []string {
	raw, ok := os.LookupEnv(key)
	if !ok || strings.TrimSpace(raw) == "" {
		return append([]string(nil), def...)
	}
	s := strings.TrimSpace(raw)
	// Try JSON first
	if strings.HasPrefix(s, "[") {
		var arr []string
		if err := json.Unmarshal([]byte(s), &arr); err == nil {
			return compactStrings(arr)
		}
	}
	// Fallback: CSV
	parts := strings.Split(s, ",")
	return compactStrings(parts)
}

// GetEnvMap lee un mapa desde el env. Acepta:
// - JSON object: {"iss1":"aud1","iss2":"aud2"}
// - Lista "k=v" separada por ';' o ',' : "iss1=aud1;iss2=aud2"
func GetEnvMap(key string, def map[string]string) map[string]string {
	raw, ok := os.LookupEnv(key)
	out := make(map[string]string, len(def))
	for k, v := range def { // copia del default
		out[k] = v
	}
	if !ok || strings.TrimSpace(raw) == "" {
		return out
	}
	s := strings.TrimSpace(raw)

	// 1) JSON object
	if strings.HasPrefix(s, "{") {
		var m map[string]string
		if err := json.Unmarshal([]byte(s), &m); err == nil {
			for k, v := range m {
				k = strings.TrimSpace(k)
				v = strings.TrimSpace(v)
				if k != "" && v != "" {
					out[k] = v
				}
			}
			return out
		}
		log.Printf("WARN: %s no es JSON válido, se intenta como lista k=v", key)
	}

	// 2) "k=v" separados por ';' o ','
	sep := ";"
	if !strings.Contains(s, ";") && strings.Contains(s, ",") {
		sep = ","
	}
	for _, p := range strings.Split(s, sep) {
		p = strings.TrimSpace(p)
		if p == "" {
			continue
		}
		kv := strings.SplitN(p, "=", 2)
		if len(kv) != 2 {
			log.Printf("WARN: %s: par inválido %q (se omite)", key, p)
			continue
		}
		k := strings.TrimSpace(kv[0])
		v := strings.TrimSpace(kv[1])
		if k != "" && v != "" {
			out[k] = v
		}
	}
	return out
}

// GetEnvSet builds a set (map[string]struct{}) from an env list.
func GetEnvSet(key string, def []string) map[string]struct{} {
	out := make(map[string]struct{})
	for _, v := range GetEnvList(key, def) {
		out[v] = struct{}{}
	}
	return out
}

func compactStrings(in []string) []string {
	out := make([]string, 0, len(in))
	for _, v := range in {
		v = strings.TrimSpace(v)
		if v != "" {
			out = append(out, v)
		}
	}
	return out
}
