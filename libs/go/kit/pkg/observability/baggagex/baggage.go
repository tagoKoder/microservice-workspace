package baggagex

import (
	"context"
	"strings"

	"go.opentelemetry.io/otel/baggage"
)

// Value obtiene un valor del baggage por clave.
func Value(ctx context.Context, key string) (string, bool) {
	m := baggage.FromContext(ctx).Member(key)
	if m.Key() == "" {
		return "", false
	}
	return m.Value(), true
}

// Tags devuelve pares k/v filtrados por prefijo/allow/deny y limita cantidad y tamaño.
func Tags(ctx context.Context, prefix string, allow, deny map[string]struct{}, maxKeys, maxVal int) map[string]string {
	bg := baggage.FromContext(ctx)
	if bg.Len() == 0 {
		return nil
	}
	out := make(map[string]string, bg.Len())
	n := 0
	repl := strings.NewReplacer(".", "_", "-", "_")
	for _, m := range bg.Members() {
		k := m.Key()
		if prefix != "" && !strings.HasPrefix(k, prefix) {
			continue
		}
		if allow != nil {
			if _, ok := allow[k]; !ok {
				continue
			}
		}
		if deny != nil {
			if _, ok := deny[k]; ok {
				continue
			}
		}
		v := m.Value()
		if maxVal > 0 && len(v) > maxVal {
			v = v[:maxVal] + "…"
		}
		out[repl.Replace(k)] = v
		n++
		if maxKeys > 0 && n >= maxKeys {
			break
		}
	}
	return out
}
