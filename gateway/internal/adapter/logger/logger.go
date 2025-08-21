package logger

import (
	"os"
	"time"

	"github.com/rs/zerolog"
)

func New() zerolog.Logger {
	l := zerolog.New(os.Stdout).
		With().
		Timestamp().
		Str("component", "gateway").
		Logger()
	zerolog.DurationFieldUnit = time.Millisecond
	return l
}
