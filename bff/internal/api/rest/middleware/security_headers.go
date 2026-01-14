package middleware

import "net/http"

func SecurityHeaders(hsts bool) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("X-Content-Type-Options", "nosniff")
			w.Header().Set("X-Frame-Options", "DENY")
			w.Header().Set("Referrer-Policy", "no-referrer")
			w.Header().Set("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
			// CSP base (ajústala a tu frontend)
			w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")

			if hsts {
				// Solo cuando sirves por HTTPS
				w.Header().Set("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
			}
			next.ServeHTTP(w, r)
		})
	}
}
