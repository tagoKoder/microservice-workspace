package slogx

import (
	"log/slog"
	"sort"
	"strings"
)

// MapToArgs aplana un map[string]any a pares "k", v ordenados alfabéticamente.
// Útil para logger.Info("msg", MapToArgs(m)...)
func MapToArgs(m map[string]any) []any {
	if len(m) == 0 {
		return nil
	}
	keys := make([]string, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	out := make([]any, 0, len(keys)*2)
	for _, k := range keys {
		out = append(out, sanitizeKey(k), m[k])
	}
	return out
}

// MapToGroup empaqueta el mapa en un slog.Group conservando anidación.
// Útil para logger.Info("msg", MapToGroup("config", m))
func MapToGroup(name string, m map[string]any) slog.Attr {
	return slog.Group(sanitizeKey(name), mapToAny(m)...)
}

func MapToAttr(m map[string]any) slog.Attr {
	return slog.Group(sanitizeKey("config"), mapToAny(m)...)
}

// mapToAny convierte map[string]any -> []any usando slog.Any y slog.Group (recursivo).
func mapToAny(m map[string]any) []any {
	if len(m) == 0 {
		return nil
	}
	keys := make([]string, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	out := make([]any, 0, len(keys)*2)
	for _, k := range keys {
		key := sanitizeKey(k)
		switch v := m[k].(type) {
		case map[string]any:
			out = append(out, slog.Group(key, mapToAny(v)...))
		default:
			out = append(out, slog.Any(key, v))
		}
	}
	return out
}

func sanitizeKey(k string) string {
	return strings.NewReplacer(" ", "_", ".", "_", "-", "_").Replace(k)
}
