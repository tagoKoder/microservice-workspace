package validation

import (
	"fmt"
	"regexp"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/google/uuid"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// =====================
// gRPC error helpers
// =====================

func Invalid(msg string) error   { return status.Error(codes.InvalidArgument, msg) }
func Forbidden(msg string) error { return status.Error(codes.PermissionDenied, msg) }
func Unauth(msg string) error    { return status.Error(codes.Unauthenticated, msg) }

// =====================
// string safety (ASVS L3: prevent log/header injection, control chars)
// =====================

func hasControlChars(s string) bool {
	for _, r := range s {
		if r <= 0x1F || r == 0x7F {
			return true
		}
	}
	return false
}

func RequireNonBlank(raw, field string, maxLen int) (string, error) {
	if raw == "" {
		return "", Invalid(field + " is required")
	}
	s := strings.TrimSpace(raw)
	if s == "" {
		return "", Invalid(field + " is required")
	}
	if hasControlChars(s) {
		return "", Invalid(field + " contains invalid characters")
	}
	if maxLen > 0 && utf8.RuneCountInString(s) > maxLen {
		return "", Invalid(field + " too long")
	}
	return s, nil
}

func OptionalTrim(raw, field string, maxLen int) (string, error) {
	if raw == "" {
		return "", nil
	}
	s := strings.TrimSpace(raw)
	if s == "" {
		return "", nil
	}
	if hasControlChars(s) {
		return "", Invalid(field + " contains invalid characters")
	}
	if maxLen > 0 && utf8.RuneCountInString(s) > maxLen {
		return "", Invalid(field + " too long")
	}
	return s, nil
}

// =====================
// UUID / Currency / Idempotency
// =====================

func RequireUUID(raw, field string) (uuid.UUID, error) {
	s, err := RequireNonBlank(raw, field, 64)
	if err != nil {
		return uuid.Nil, err
	}
	id, e := uuid.Parse(s)
	if e != nil {
		return uuid.Nil, Invalid(field + " must be a UUID")
	}
	return id, nil
}

var reCurrency = regexp.MustCompile(`^[A-Z]{3}$`)

func RequireCurrency(raw string) (string, error) {
	s, err := RequireNonBlank(raw, "currency", 3)
	if err != nil {
		return "", err
	}
	s = strings.ToUpper(s)
	if !reCurrency.MatchString(s) {
		return "", Invalid("currency must be ISO-4217 (e.g., USD)")
	}
	return s, nil
}

var reIdem = regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9._:-]{7,127}$`)

func RequireIdempotencyKey(raw, field string) (string, error) {
	s, err := RequireNonBlank(raw, field, 128)
	if err != nil {
		return "", err
	}
	if !reIdem.MatchString(s) {
		return "", Invalid(field + " invalid")
	}
	return s, nil
}

// =====================
// Actor / identifiers (initiated_by / external_ref / reason)
// =====================

// initiated_by suele ser subject/actor. Permitimos un set seguro (sin espacios/control chars).
var reActor = regexp.MustCompile(`^[A-Za-z0-9:_@./-]{1,200}$`)

func RequireActor(raw string) (string, error) {
	s, err := RequireNonBlank(raw, "initiated_by", 200)
	if err != nil {
		return "", err
	}
	if !reActor.MatchString(s) {
		return "", Invalid("initiated_by invalid")
	}
	return s, nil
}

// external_ref / reason: opcional, limitado para evitar DoS e inyección.
var reSafeRef = regexp.MustCompile(`^[A-Za-z0-9:_@./#-]{1,140}$`)

func OptionalExternalRef(raw string) (string, error) {
	s, err := OptionalTrim(raw, "external_ref", 140)
	if err != nil {
		return "", err
	}
	if s == "" {
		return "", nil
	}
	if !reSafeRef.MatchString(s) {
		return "", Invalid("external_ref invalid")
	}
	return s, nil
}

func OptionalReason(raw string) (string, error) {
	s, err := OptionalTrim(raw, "reason", 64)
	if err != nil {
		return "", err
	}
	if s == "" {
		return "", nil
	}
	// ejemplo: registration_bonus, payment_hold, etc.
	if !regexp.MustCompile(`^[a-z0-9_]{2,64}$`).MatchString(strings.ToLower(s)) {
		return "", Invalid("reason invalid")
	}
	return strings.ToLower(s), nil
}

// =====================
// Decimal amount (ASVS: no float, no overflow, format strict)
// =====================

// 20,6 (hasta 20 dígitos antes del punto, hasta 6 decimales).
var reAmount = regexp.MustCompile(`^(?:0|[1-9]\d{0,19})(?:\.\d{1,6})?$`)

func RequireDecimalAmount(raw, field string) (string, error) {
	s, err := RequireNonBlank(raw, field, 32)
	if err != nil {
		return "", err
	}
	if !reAmount.MatchString(s) {
		return "", Invalid(field + " must be a decimal string (max 20,6)")
	}
	// opcional: evita montos absurdos (DoS/overflow semántico)
	// aquí solo un guard básico por longitud ya lo hace, pero puedes endurecer:
	return s, nil
}

// =====================
// Timestamps & ranges (DoS guard)
// =====================

func RequireTimestamp(ts *timestamppb.Timestamp, field string) (time.Time, error) {
	if ts == nil {
		return time.Time{}, Invalid(field + " is required")
	}
	if !ts.IsValid() {
		return time.Time{}, Invalid(field + " invalid")
	}
	t := ts.AsTime().UTC()
	// guard: no aceptar fechas ridículas (p. ej. año < 2000 o > 2100)
	if t.Year() < 2000 || t.Year() > 2100 {
		return time.Time{}, Invalid(field + " out of range")
	}
	return t, nil
}

func ValidateTimeWindow(from, to time.Time, maxRange time.Duration) error {
	if to.Before(from) {
		return Invalid("to must be >= from")
	}
	if maxRange > 0 && to.Sub(from) > maxRange {
		return Invalid(fmt.Sprintf("time range too large (max %s)", maxRange))
	}
	return nil
}

// =====================
// Pagination (ASVS: bounds to prevent DoS)
// =====================

func NormalizePageSize(page, size int32, maxSize int32, oneBased bool) (int, int, error) {
	p := int(page)
	s := int(size)

	if oneBased {
		if p <= 0 {
			p = 1
		}
	} else {
		if p < 0 {
			return 0, 0, Invalid("page must be >= 0")
		}
	}

	if s <= 0 {
		s = 50
	}
	if s > int(maxSize) {
		return 0, 0, Invalid(fmt.Sprintf("size max %d", maxSize))
	}
	return p, s, nil
}
