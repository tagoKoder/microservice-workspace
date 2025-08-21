package grpcx

import (
	"context"
	"fmt"
	"strings"

	"google.golang.org/grpc/metadata"
)

// Set de headers sensibles (configurable si quieres)
var SensitiveHeaders = map[string]struct{}{
	"authorization": {},
	"cookie":        {},
	"set-cookie":    {},
	"x-api-key":     {},
}

// SanitizeMetadata devuelve un map con metadatos en minúsculas, sin binarios ni sensibles.
func SanitizeMetadata(ctx context.Context) map[string]string {
	out := map[string]string{}
	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		return out
	}
	for k, v := range md {
		kl := strings.ToLower(k)
		if strings.HasSuffix(kl, "-bin") {
			continue
		}
		if _, sensitive := SensitiveHeaders[kl]; sensitive {
			continue
		}
		switch len(v) {
		case 0:
			// nada
		case 1:
			out[kl] = v[0]
		default:
			out[kl] = fmt.Sprintf("%v", v)
		}
	}
	return out
}

// MetadataMap retorna TODOS los metadatos (no saneados), útil para debug puntual.
func MetadataMap(ctx context.Context) map[string]string {
	out := map[string]string{}
	if md, ok := metadata.FromIncomingContext(ctx); ok {
		for k, v := range md {
			if len(v) == 1 {
				out[k] = v[0]
			} else if len(v) > 1 {
				out[k] = fmt.Sprintf("%v", v)
			}
		}
	}
	return out
}
