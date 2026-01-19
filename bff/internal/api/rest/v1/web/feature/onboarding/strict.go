package onboarding

import (
	"context"
	"time"

	openapi "github.com/tagoKoder/bff/internal/api/rest/gen/openapi"
	"github.com/tagoKoder/bff/internal/client/ports"
)

func (h *Handler) StartOnboardingStrict(
	ctx context.Context,
	body openapi.OnboardingIntentRequest,
) (openapi.StartOnboardingResponseObject, error) {

	channel := "web"
	if body.Channel != nil && *body.Channel != "" {
		channel = *body.Channel
	}

	idCT := ""
	if body.IdFrontContentType != nil {
		idCT = *body.IdFrontContentType
	}
	selfieCT := ""
	if body.SelfieContentType != nil {
		selfieCT = *body.SelfieContentType
	}

	out, err := h.clients.Identity.StartRegistration(ctx, ports.StartRegistrationInput{
		Channel:             channel,
		Email:               string(body.Email),
		Phone:               body.Phone,
		NationalID:          body.NationalId,
		NationalIDIssueDate: body.NationalIdIssueDate,
		FingerprintCode:     body.FingerprintCode,
		MonthlyIncome:       float64(body.MonthlyIncome),
		OccupationType:      string(body.OccupationType),
		IdFrontContentType:  idCT,
		SelfieContentType:   selfieCT,
	})
	if err != nil {
		return openapi.StartOnboarding502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "identity service unavailable",
		}), nil
	}

	uploads := make([]openapi.PresignedUpload, 0, len(out.Uploads))
	for _, u := range out.Uploads {
		hh := make([]openapi.PresignedHeader, 0, len(u.Headers))
		for _, hhh := range u.Headers {
			hh = append(hh, openapi.PresignedHeader{Name: hhh.Name, Value: hhh.Value})
		}

		docType := openapi.KycDocType("unspecified")
		if u.DocType == "id_front" {
			docType = openapi.KycDocType("id_front")
		} else if u.DocType == "selfie" {
			docType = openapi.KycDocType("selfie")
		}

		uploads = append(uploads, openapi.PresignedUpload{
			DocType:          docType,
			Bucket:           u.Bucket,
			Key:              u.Key,
			UploadUrl:        u.UploadURL,
			Headers:          hh,
			MaxBytes:         u.MaxBytes,
			ContentType:      u.ContentType,
			ExpiresInSeconds: u.ExpiresInSeconds,
		})
	}

	state := openapi.RegistrationState("unspecified")
	switch out.State {
	case "started":
		state = openapi.RegistrationState("started")
	case "contact_verified":
		state = openapi.RegistrationState("contact_verified")
	case "consented":
		state = openapi.RegistrationState("consented")
	case "activated":
		state = openapi.RegistrationState("activated")
	case "rejected":
		state = openapi.RegistrationState("rejected")
	}

	var createdAtPtr *time.Time
	if out.CreatedAtRFC3339 != "" {
		if t, e := time.Parse(time.RFC3339, out.CreatedAtRFC3339); e == nil {
			createdAtPtr = &t
		}
	}

	resp := openapi.OnboardingIntentResponse{
		RegistrationId: out.RegistrationID,
		State:          state,
		Uploads:        uploads,
		CreatedAt:      createdAtPtr,
		// otp_channel_hint queda opcional/legacy
	}

	return openapi.StartOnboarding201JSONResponse(resp), nil
}

func (h *Handler) ConfirmOnboardingKycStrict(
	ctx context.Context,
	body openapi.ConfirmKycRequest,
) (openapi.ConfirmOnboardingKycResponseObject, error) {

	channel := "web"
	if body.Channel != nil && *body.Channel != "" {
		channel = *body.Channel
	}

	objects := make([]ports.UploadedObject, 0, len(body.Objects))
	for _, o := range body.Objects {
		etag := ""
		if o.Etag != nil {
			etag = *o.Etag
		}
		ct := ""
		if o.ContentType != nil {
			ct = *o.ContentType
		}
		size := int64(0)
		if o.SizeBytes != nil {
			size = *o.SizeBytes
		}

		objects = append(objects, ports.UploadedObject{
			DocType:     string(o.DocType),
			Bucket:      o.Bucket,
			Key:         o.Key,
			ETag:        etag,
			SizeBytes:   size,
			ContentType: ct,
		})
	}

	out, err := h.clients.Identity.ConfirmRegistrationKyc(ctx, ports.ConfirmRegistrationKycInput{
		RegistrationID: body.RegistrationId,
		Objects:        objects,
		Channel:        channel,
	})
	if err != nil {
		return openapi.ConfirmOnboardingKyc502JSONResponse(openapi.ErrorResponse{
			Code:    "UPSTREAM_ERROR",
			Message: "identity service unavailable",
		}), nil
	}

	state := openapi.RegistrationState("unspecified")
	switch out.State {
	case "started":
		state = openapi.RegistrationState("started")
	case "contact_verified":
		state = openapi.RegistrationState("contact_verified")
	case "consented":
		state = openapi.RegistrationState("consented")
	case "activated":
		state = openapi.RegistrationState("activated")
	case "rejected":
		state = openapi.RegistrationState("rejected")
	}

	statuses := make([]openapi.KycObjectStatus, 0, len(out.Statuses))
	for _, st := range out.Statuses {
		docType := openapi.KycDocType("unspecified")
		if st.DocType == "id_front" {
			docType = openapi.KycDocType("id_front")
		} else if st.DocType == "selfie" {
			docType = openapi.KycDocType("selfie")
		}

		status := openapi.KycUploadStatus("unspecified")
		if st.Status == "pending" {
			status = openapi.KycUploadStatus("pending")
		} else if st.Status == "confirmed" {
			status = openapi.KycUploadStatus("confirmed")
		}

		var etagPtr *string
		if st.ETag != "" {
			etag := st.ETag
			etagPtr = &etag
		}

		statuses = append(statuses, openapi.KycObjectStatus{
			DocType: docType,
			Status:  status,
			Bucket:  st.Bucket,
			Key:     st.Key,
			Etag:    etagPtr,
		})
	}

	var confirmedAtPtr *time.Time
	if out.ConfirmedAtRFC3339 != "" {
		if t, e := time.Parse(time.RFC3339, out.ConfirmedAtRFC3339); e == nil {
			confirmedAtPtr = &t
		}
	}

	return openapi.ConfirmOnboardingKyc200JSONResponse(openapi.ConfirmKycResponse{
		RegistrationId: out.RegistrationID,
		State:          state,
		Statuses:       statuses,
		ConfirmedAt:    confirmedAtPtr,
	}), nil
}

func (h *Handler) ActivateOnboardingStrict(
	ctx context.Context,
	body openapi.ActivateRequest,
) (openapi.ActivateOnboardingResponseObject, error) {

	// STUB: aún no hay RPC/servicio mostrado para activar (crear customer + account).
	// Deja el contrato listo y evita romper el build.
	return openapi.ActivateOnboarding502JSONResponse(openapi.ErrorResponse{
		Code:    "NOT_IMPLEMENTED",
		Message: "activate onboarding not implemented in BFF yet",
	}), nil
}
