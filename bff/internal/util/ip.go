// bff/internal/util/ip.go
package util

import (
	"net"
	"net/http"
	"strings"
)

func GetClientIP(r *http.Request) string {
	// 0) Si ya usas chi middleware.RealIP, RemoteAddr suele venir "ip" sin puerto.
	// Igual lo normalizamos por si acaso.
	if ip := NormalizeIP(r.RemoteAddr); ip != "" {
		return ip
	}

	// 1) X-Forwarded-For (puede venir "ip, proxy1, proxy2" y/o con puerto)
	if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
		first := strings.TrimSpace(strings.Split(xff, ",")[0])
		if ip := NormalizeIP(first); ip != "" {
			return ip
		}
	}

	// 2) X-Real-IP (también puede venir con puerto)
	if xr := r.Header.Get("X-Real-IP"); xr != "" {
		if ip := NormalizeIP(xr); ip != "" {
			return ip
		}
	}

	return ""
}

func NormalizeIP(v string) string {
	v = strings.TrimSpace(v)
	if v == "" {
		return ""
	}

	// Si viene host:port o [ipv6]:port → separa
	if host, _, err := net.SplitHostPort(v); err == nil {
		v = host
	}

	// Quita brackets de IPv6
	v = strings.Trim(v, "[]")

	// Valida que sea IP real
	if ip := net.ParseIP(v); ip != nil {
		return ip.String()
	}

	return ""
}
