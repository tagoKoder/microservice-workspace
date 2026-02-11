// bff\internal\api\rest\middleware\access_log.go
package middleware

import (
	"log"
	"net/http"
	"time"

	"github.com/tagoKoder/bff/internal/security"
)

type statusWriter struct {
	http.ResponseWriter
	status int
}

func (w *statusWriter) WriteHeader(code int) {
	w.status = code
	w.ResponseWriter.WriteHeader(code)
}

func AccessLog() func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			sw := &statusWriter{ResponseWriter: w, status: 200}
			start := time.Now()
			cid := security.CorrelationID(r.Context())
			rt := GetRouteTemplate(r.Context())
			if rt == "" {
				rt = r.URL.Path // fallback
			}
			log.Printf("cid=%s %s %s -> %d (%s)", cid, r.Method, rt, sw.status, time.Since(start))
			next.ServeHTTP(sw, r)
		})
	}
}
