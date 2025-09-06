package gormx

import (
	"context"
	"time"

	logx "github.com/tagoKoder/common-kit/pkg/logging"
	glogger "gorm.io/gorm/logger"
)

type LogxAdapter struct {
	level         glogger.LogLevel
	slowThreshold time.Duration
}

func NewLogxAdapter(level glogger.LogLevel, slow time.Duration) glogger.Interface {
	if slow <= 0 {
		slow = 200 * time.Millisecond
	}
	return &LogxAdapter{level: level, slowThreshold: slow}
}

func (a *LogxAdapter) LogMode(l glogger.LogLevel) glogger.Interface {
	return &LogxAdapter{level: l, slowThreshold: a.slowThreshold}
}

func (a *LogxAdapter) Info(ctx context.Context, msg string, data ...any) {
	if a.level >= glogger.Info {
		logx.From(ctx).Info(msg, "data", data)
	}
}
func (a *LogxAdapter) Warn(ctx context.Context, msg string, data ...any) {
	if a.level >= glogger.Warn {
		logx.From(ctx).Warn(msg, "data", data)
	}
}
func (a *LogxAdapter) Error(ctx context.Context, msg string, data ...any) {
	if a.level >= glogger.Error {
		logx.From(ctx).Error(msg, "data", data)
	}
}

func (a *LogxAdapter) Trace(ctx context.Context, begin time.Time, fc func() (string, int64), err error) {
	if a.level == glogger.Silent {
		return
	}
	elapsed := time.Since(begin)
	sql, rows := fc()

	kv := []any{"sql", sql, "rows", rows, "elapsed", elapsed}
	l := logx.From(ctx)
	switch {
	case err != nil:
		l.Error("gorm", append(kv, "err", err)...)
	case a.level >= glogger.Info && elapsed > a.slowThreshold:
		l.Warn("gorm slow", kv...)
	case a.level >= glogger.Info:
		l.Info("gorm", kv...)
	}
}
