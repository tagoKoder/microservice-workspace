// bff\internal\api\rest\httperr\httperr.go
package httperr

import (
	"encoding/json"
	"errors"
	"net/http"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type ErrorResponse struct {
	Code    string         `json:"code"`
	Message string         `json:"message"`
	Details map[string]any `json:"details,omitempty"`
}

// AppError: error con “cara pública” y causa interna.
type AppError struct {
	HTTPStatus int
	PublicCode string
	PublicMsg  string
	PublicDet  map[string]any
	Cause      error
}

func (e *AppError) Error() string {
	if e.Cause != nil {
		return e.PublicCode + ": " + e.Cause.Error()
	}
	return e.PublicCode
}

func (e *AppError) Unwrap() error { return e.Cause }

func New(status int, code, msg string) *AppError {
	return &AppError{HTTPStatus: status, PublicCode: code, PublicMsg: msg}
}

func (e *AppError) WithCause(err error) *AppError          { e.Cause = err; return e }
func (e *AppError) WithDetails(d map[string]any) *AppError { e.PublicDet = d; return e }

// Helpers comunes (alineados a tu OpenAPI)
func BadRequest() *AppError   { return New(http.StatusBadRequest, "BAD_REQUEST", "Invalid request") }
func Unauthorized() *AppError { return New(http.StatusUnauthorized, "UNAUTHORIZED", "Unauthorized") }
func Forbidden() *AppError    { return New(http.StatusForbidden, "FORBIDDEN", "Forbidden") }
func NotFound() *AppError     { return New(http.StatusNotFound, "NOT_FOUND", "Not found") }
func Conflict() *AppError     { return New(http.StatusConflict, "CONFLICT", "Conflict") }
func Upstream() *AppError {
	return New(http.StatusBadGateway, "UPSTREAM_ERROR", "Upstream service error")
}
func Internal() *AppError {
	return New(http.StatusInternalServerError, "INTERNAL", "Internal server error")
}

// MapGrpc: mapea códigos gRPC a HTTP sin filtrar mensajes internos.
func MapGrpc(err error) *AppError {
	st, ok := status.FromError(err)
	if !ok {
		return Internal().WithCause(err)
	}

	switch st.Code() {
	case codes.InvalidArgument, codes.FailedPrecondition, codes.OutOfRange:
		return BadRequest().WithCause(err)
	case codes.Unauthenticated:
		return Unauthorized().WithCause(err)
	case codes.PermissionDenied:
		return Forbidden().WithCause(err)
	case codes.NotFound:
		return NotFound().WithCause(err)
	case codes.AlreadyExists, codes.Aborted:
		return Conflict().WithCause(err)
	case codes.Unavailable, codes.DeadlineExceeded:
		return Upstream().WithCause(err)
	default:
		return Internal().WithCause(err)
	}
}

// Normalize: convierte cualquier error en AppError (sanitizado).
func Normalize(err error) *AppError {
	if err == nil {
		return nil
	}

	var ae *AppError
	if errors.As(err, &ae) {
		return ae
	}

	// gRPC (la mayoría de tus errores “reales” vendrán de aquí)
	if _, ok := status.FromError(err); ok {
		return MapGrpc(err)
	}

	// fallback
	return Internal().WithCause(err)
}

// Write: responde JSON sanitizado + setea headers mínimos
// debug=false => NO incluir details ni mensajes internos
func Write(w http.ResponseWriter, r *http.Request, err error, debug bool) {
	ae := Normalize(err)

	resp := ErrorResponse{
		Code:    ae.PublicCode,
		Message: ae.PublicMsg,
	}

	if debug && ae.PublicDet != nil {
		resp.Details = ae.PublicDet
	}

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(ae.HTTPStatus)
	_ = json.NewEncoder(w).Encode(resp)
}
