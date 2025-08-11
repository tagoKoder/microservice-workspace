package gorm

import (
	"context"
	"time"

	"log/slog"

	glogger "gorm.io/gorm/logger"
)

type slogGormLogger struct {
	l             *slog.Logger
	level         glogger.LogLevel
	slowThreshold time.Duration
}

func NewSlogGormLogger(l *slog.Logger, level glogger.LogLevel, slow time.Duration) glogger.Interface {
	if l == nil {
		l = slog.Default()
	}
	if slow <= 0 {
		slow = 200 * time.Millisecond
	}
	return &slogGormLogger{l: l, level: level, slowThreshold: slow}
}

func (s *slogGormLogger) LogMode(level glogger.LogLevel) glogger.Interface {
	return &slogGormLogger{l: s.l, level: level, slowThreshold: s.slowThreshold}
}

func (s *slogGormLogger) Info(ctx context.Context, msg string, data ...interface{}) {
	if s.level >= glogger.Info {
		s.l.Info(msg, "data", data)
	}
}
func (s *slogGormLogger) Warn(ctx context.Context, msg string, data ...interface{}) {
	if s.level >= glogger.Warn {
		s.l.Warn(msg, "data", data)
	}
}
func (s *slogGormLogger) Error(ctx context.Context, msg string, data ...interface{}) {
	if s.level >= glogger.Error {
		s.l.Error(msg, "data", data)
	}
}

func (s *slogGormLogger) Trace(ctx context.Context, begin time.Time, fc func() (string, int64), err error) {
	if s.level == glogger.Silent {
		return
	}
	elapsed := time.Since(begin)
	sql, rows := fc()
	attrs := []any{"rows", rows, "elapsed", elapsed}

	switch {
	case err != nil:
		s.l.Error("gorm", append(attrs, "err", err, "sql", sql)...)
	case s.level >= glogger.Info && s.slowThreshold > 0 && elapsed > s.slowThreshold:
		s.l.Warn("gorm slow", append(attrs, "sql", sql)...)
	case s.level >= glogger.Info:
		s.l.Info("gorm", append(attrs, "sql", sql)...)
	}
}
