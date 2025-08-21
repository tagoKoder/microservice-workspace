package sysx

import "os"

// GuessService devuelve un nombre razonable para identificar la instancia/servicio.
func GuessService() string {
	if hn, _ := os.Hostname(); hn != "" {
		return hn
	}
	return "unknown"
}
