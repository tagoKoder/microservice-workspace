package httpx

import (
	"encoding/json"
	"net/http"
)

type ErrBody struct {
	Error      string `json:"error"`
	RequestID  string `json:"request_id,omitempty"`
	StatusCode int    `json:"-"`
}

func JSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func Error(w http.ResponseWriter, status int, msg, reqID string) {
	JSON(w, status, ErrBody{Error: msg, RequestID: reqID, StatusCode: status})
}
