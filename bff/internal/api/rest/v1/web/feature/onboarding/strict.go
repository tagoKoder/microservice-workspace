package onboarding

import (
	"context"
	"io"
	"net/http"
	"strconv"
	"strings"

	"github.com/google/uuid"
	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/client/ports"
)

const (
	maxMemory      = 8 << 20 // memoria para ParseMultipartForm
	maxFilePerPart = 6 << 20 // 6MB por archivo
)

func (h *Handler) IntentsMultipartStrict(
	ctx context.Context,
	r *http.Request,
) (openapi.StartOnboardingResponseObject, error) {

	if err := r.ParseMultipartForm(maxMemory); err != nil {
		return openapi.StartOnboarding400JSONResponse(openapi.ErrorResponse{
			Code:    "BAD_REQUEST",
			Message: "invalid multipart form",
		}), nil
	}

	get := func(k string) string { return strings.TrimSpace(r.FormValue(k)) }

	email := get("email")
	phone := get("phone")
	channel := get("channel")
	if channel == "" {
		channel = "web"
	}

	nationalID := get("national_id")
	issueDate := get("national_id_issue_date")
	fingerprint := get("fingerprint_code")
	occupation := get("occupation_type")

	miStr := get("monthly_income")
	monthlyIncome, err := strconv.ParseFloat(miStr, 64)
	if err != nil {
		return openapi.StartOnboarding422JSONResponse(openapi.ErrorResponse{
			Code:    "VALIDATION",
			Message: "monthly_income must be a number",
		}), nil
	}

	idFile, _, err := r.FormFile("id_document_front")
	if err != nil {
		return openapi.StartOnboarding422JSONResponse(openapi.ErrorResponse{
			Code:    "VALIDATION",
			Message: "id_document_front is required",
		}), nil
	}
	defer idFile.Close()

	selfieFile, _, err := r.FormFile("selfie")
	if err != nil {
		return openapi.StartOnboarding422JSONResponse(openapi.ErrorResponse{
			Code:    "VALIDATION",
			Message: "selfie is required",
		}), nil
	}
	defer selfieFile.Close()

	idBytes, err := io.ReadAll(io.LimitReader(idFile, maxFilePerPart))
	if err != nil || len(idBytes) == 0 {
		return openapi.StartOnboarding400JSONResponse(openapi.ErrorResponse{
			Code:    "BAD_REQUEST",
			Message: "cannot read id_document_front",
		}), nil
	}

	selfieBytes, err := io.ReadAll(io.LimitReader(selfieFile, maxFilePerPart))
	if err != nil || len(selfieBytes) == 0 {
		return openapi.StartOnboarding400JSONResponse(openapi.ErrorResponse{
			Code:    "BAD_REQUEST",
			Message: "cannot read selfie",
		}), nil
	}

	out, err := h.clients.Identity.StartRegistration(ctx, ports.StartRegistrationInput{
		Channel:             channel,
		Email:               email,
		Phone:               phone,
		NationalID:          nationalID,
		NationalIDIssueDate: issueDate,
		FingerprintCode:     fingerprint,
		IdDocumentFront:     idBytes,
		Selfie:              selfieBytes,
		MonthlyIncome:       monthlyIncome,
		OccupationType:      occupation,
	})
	if err != nil {
		return openapi.StartOnboarding502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "identity service unavailable",
		}), nil
	}

	return openapi.StartOnboarding201JSONResponse(openapi.OnboardingIntentResponse{
		RegistrationId: out.RegistrationID,
		OtpChannelHint: "email",
	}), nil
}

func (h *Handler) VerifyContactStrict(
	ctx context.Context,
	body openapi.VerifyContactRequest,
) (openapi.VerifyOnboardingContactResponseObject, error) {
	// Tu ports.Identity no expone un RPC de verify OTP/contacto en lo que pegaste.
	// Mantengo comportamiento estable según tu OpenAPI (“stub por ahora”).
	_ = body.RegistrationId
	_ = body.Otp

	return openapi.VerifyOnboardingContact200JSONResponse(openapi.SimpleStatusResponse{Status: "ok"}), nil
}

func (h *Handler) ConsentsStrict(
	ctx context.Context,
	body openapi.ConsentsRequest,
) (openapi.RegisterOnboardingConsentsResponseObject, error) {
	// Igual: no hay RPC en ports para persistir consents en lo que pegaste.
	_ = body.RegistrationId
	_ = body.Accepted

	return openapi.RegisterOnboardingConsents200JSONResponse(openapi.SimpleStatusResponse{Status: "ok"}), nil
}

func (h *Handler) ActivateStrict(
	ctx context.Context,
	body openapi.ActivateRequest,
) (openapi.ActivateOnboardingResponseObject, error) {
	cust, err := h.clients.Accounts.CreateCustomer(ctx, ports.CreateCustomerInput{
		FullName:    body.FullName,
		BirthDate:   body.BirthDate,
		TIN:         body.Tin,
		Email:       body.Email,
		Phone:       body.Phone,
		RiskSegment: ports.RiskLow,
		Address: &ports.CustomerAddressCreate{
			Country: body.Country,
		},
	})
	if err != nil {
		return openapi.ActivateOnboarding502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "accounts service unavailable (create customer)",
		}), nil
	}

	acc, err := h.clients.Accounts.CreateAccount(ctx, ports.CreateAccountInput{
		CustomerID:  cust.CustomerID,
		ProductType: ports.ProductChecking,
		Currency:    "USD",
	})
	if err != nil {
		return openapi.ActivateOnboarding502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "accounts service unavailable (create account)",
		}), nil
	}

	resp := openapi.ActivateResponse{
		CustomerId:    cust.CustomerID,
		AccountId:     acc.AccountID,
		ActivationRef: uuid.NewString(),
	}
	return openapi.ActivateOnboarding201JSONResponse(resp), nil
}
