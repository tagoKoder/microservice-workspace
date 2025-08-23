package middleware

import (
	"fmt"
	"net/http"
	"runtime/debug"

	commonObs "github.com/tagoKoder/common-kit/pkg/observability"
)

// Se usa con r.Use(Recover)
func Recover(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if rec := recover(); rec != nil {
				var err error
				if e, ok := rec.(error); ok {
					err = e
				} else {
					err = fmt.Errorf("%v", rec)
				}
				_ = commonObs.CaptureErr(r.Context(), err, map[string]any{
					"http.method": r.Method,
					"http.path":   r.URL.Path,
					"stack":       string(debug.Stack()),
				})
				w.Header().Set("Content-Type", "application/json")
				w.WriteHeader(http.StatusInternalServerError)
				_, _ = w.Write([]byte(`{"error":"internal error"}`))
			}
		}()
		next.ServeHTTP(w, r)
	})
}
