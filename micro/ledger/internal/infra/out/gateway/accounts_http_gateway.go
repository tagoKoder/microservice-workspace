package gateway

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/google/uuid"
	"github.com/shopspring/decimal"
	"github.com/sony/gobreaker"
)

type AccountsHTTPGateway struct {
	baseURL string
	token   string
	client  *http.Client
	cb      *gobreaker.CircuitBreaker
}

func NewAccountsHTTPGateway(baseURL, token string) *AccountsHTTPGateway {
	st := gobreaker.Settings{
		Name:        "accounts-http",
		MaxRequests: 5,
		Interval:    30 * time.Second,
		Timeout:     10 * time.Second,
		ReadyToTrip: func(c gobreaker.Counts) bool { return c.ConsecutiveFailures >= 3 },
	}
	return &AccountsHTTPGateway{
		baseURL: baseURL,
		token:   token,
		client:  &http.Client{Timeout: 3 * time.Second},
		cb:      gobreaker.NewCircuitBreaker(st),
	}
}

type validateReq struct {
	SourceAccountId      uuid.UUID `json:"sourceAccountId"`
	DestinationAccountId uuid.UUID `json:"destinationAccountId"`
	Currency             string    `json:"currency"`
	Amount               float64   `json:"amount"`
}
type validateResp struct {
	Ok     bool   `json:"ok"`
	Reason string `json:"reason"`
}
type holdReq struct {
	Currency string  `json:"currency"`
	Amount   float64 `json:"amount"`
	Reason   string  `json:"reason,omitempty"`
}
type holdResp struct {
	Ok      bool    `json:"ok"`
	NewHold float64 `json:"newHold"`
}

func (g *AccountsHTTPGateway) do(ctx context.Context, method, path string, body any, out any) error {
	b, _ := json.Marshal(body)
	req, err := http.NewRequestWithContext(ctx, method, g.baseURL+path, bytes.NewReader(b))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Token", g.token)

	resp, err := g.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 300 {
		// no exponemos cuerpo por seguridad; solo reason genérica
		return fmt.Errorf("accounts gateway http %d", resp.StatusCode)
	}
	if out != nil {
		return json.NewDecoder(resp.Body).Decode(out)
	}
	return nil
}

func (g *AccountsHTTPGateway) ValidateAccountsAndLimits(ctx context.Context, source, dest uuid.UUID, currency string, amount decimal.Decimal) error {
	_, err := g.cb.Execute(func() (any, error) {
		var vr validateResp
		req := validateReq{
			SourceAccountId:      source,
			DestinationAccountId: dest,
			Currency:             currency,
		}
		af, _ := amount.Float64()
		req.Amount = af

		if err := g.do(ctx, http.MethodPost, "/api/v1/internal/accounts/validate", req, &vr); err != nil {
			return nil, err
		}
		if !vr.Ok {
			return nil, fmt.Errorf("validation failed: %s", vr.Reason)
		}
		return nil, nil
	})
	return err
}

func (g *AccountsHTTPGateway) ReserveHold(ctx context.Context, source uuid.UUID, currency string, amount decimal.Decimal) error {
	_, err := g.cb.Execute(func() (any, error) {
		req := holdReq{Currency: currency}
		af, _ := amount.Float64()
		req.Amount = af
		var hr holdResp
		return nil, g.do(ctx, http.MethodPost, fmt.Sprintf("/api/v1/internal/accounts/%s/hold/reserve", source.String()), req, &hr)
	})
	return err
}

func (g *AccountsHTTPGateway) ReleaseHold(ctx context.Context, source uuid.UUID, currency string, amount decimal.Decimal) error {
	_, err := g.cb.Execute(func() (any, error) {
		req := holdReq{Currency: currency}
		af, _ := amount.Float64()
		req.Amount = af
		var hr holdResp
		return nil, g.do(ctx, http.MethodPost, fmt.Sprintf("/api/v1/internal/accounts/%s/hold/release", source.String()), req, &hr)
	})
	return err
}
