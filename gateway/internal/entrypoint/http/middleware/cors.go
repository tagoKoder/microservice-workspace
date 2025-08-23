package middleware

import (
	"net/http"
	"strconv"

	pkgMiddleware "github.com/tagoKoder/gateway/pkg/middleware"
)

type CORSOptions struct {
	AllowedOrigins   []string
	AllowedHeaders   []string
	AllowedMethods   []string
	AllowCredentials bool
	MaxAgeSeconds    int
}

var defaultCORSOptions = []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"}
var defaultCORSHeaders = []string{"Authorization", "Content-Type", "X-ID-Token"}

// Se usa con r.Use(CORS(opts))
func CORS(opts CORSOptions) func(http.Handler) http.Handler {
	allowMethods := pkgMiddleware.JoinOrDefault(opts.AllowedMethods, defaultCORSOptions)
	baseHeaders := pkgMiddleware.JoinOrDefault(opts.AllowedHeaders, defaultCORSHeaders)

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			origin := r.Header.Get("Origin")

			if opts.AllowedOrigins[0] == "*" {
				w.Header().Set("Access-Control-Allow-Origin", "*")
			} else if origin != "" && pkgMiddleware.Contains(opts.AllowedOrigins, origin) {
				w.Header().Set("Access-Control-Allow-Origin", origin)
			}

			if opts.AllowCredentials {
				w.Header().Set("Access-Control-Allow-Credentials", "true")
			}

			reqHeaders := r.Header.Get("Access-Control-Request-Headers")
			switch {
			case baseHeaders != "" && reqHeaders != "":
				w.Header().Set("Access-Control-Allow-Headers", baseHeaders+","+reqHeaders)
			case baseHeaders != "":
				w.Header().Set("Access-Control-Allow-Headers", baseHeaders)
			case reqHeaders != "":
				w.Header().Set("Access-Control-Allow-Headers", reqHeaders)
			}

			w.Header().Set("Access-Control-Allow-Methods", allowMethods)

			if opts.MaxAgeSeconds > 0 {
				w.Header().Set("Access-Control-Max-Age", strconv.Itoa(opts.MaxAgeSeconds))
			}

			if r.Method == http.MethodOptions {
				w.WriteHeader(http.StatusNoContent)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
