package com.tagokoder.account.infra.in.grpc.validation;

import bank.accounts.v1.*;
import com.tagokoder.account.domain.port.in.CreateCustomerUseCase;
import com.tagokoder.account.domain.port.in.PatchCustomerUseCase;
import com.tagokoder.account.infra.in.grpc.mapper.ProtoEnumMapper;

import java.time.LocalDate;
import java.util.UUID;

import static com.tagokoder.account.infra.in.grpc.validation.GrpcValidation.*;

public final class CustomersGrpcValidators {
    private CustomersGrpcValidators() {}

    public static CreateCustomerUseCase.Command toCreateCustomerCommand(CreateCustomerRequest req) {

        // Campos core
        String fullName = requireNonBlank(req.getFullName(), "full_name");
        if (fullName.length() > 120) throw invalid("full_name too long");

        LocalDate birthDate = requireIsoDate(req.getBirthDate(), "birth_date");

        String tin = requireNonBlank(req.getTin(), "tin");
        if (tin.length() > 32) throw invalid("tin too long");
        // defensivo: evita payloads raros
        if (!tin.matches("^[A-Za-z0-9\\-]+$")) throw invalid("tin invalid");

        String riskSeg = ProtoEnumMapper.toDbRiskSegment(req.getRiskSegment()); // default low

        // contact (opcional pero recomendado)
        String email = optionalEmail(req.getEmail(), "email");
        String phone = optionalPhone(req.getPhone(), "phone");
        // si tu negocio requiere al menos uno:
        // if (email == null && phone == null) throw invalid("email or phone is required");

        // Address opcional (validación defensiva + longitudes)
        CreateCustomerUseCase.Address addr = null;
        if (req.hasAddress()) {
            var a = req.getAddress();
            String country = requireCountry2(a.getCountry(), "address.country");
            String line1 = optionalTrim(a.getLine1(), "address.line1", 120);
            String line2 = optionalTrim(a.getLine2(), "address.line2", 120);
            String city = optionalTrim(a.getCity(), "address.city", 80);
            String province = optionalTrim(a.getProvince(), "address.province", 80);
            String postal = optionalTrim(a.getPostalCode(), "address.postal_code", 16);

            addr = new CreateCustomerUseCase.Address(country, line1, line2, city, province, postal);
        }

        // idempotency_key recomendado -> si viene, validar formato seguro
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            requireIdempotencyKey(req.getIdempotencyKey(), "idempotency_key");
        }
        optionalExternalRef(req.getExternalRef(), "external_ref");

        return new CreateCustomerUseCase.Command(
                fullName,
                birthDate,
                tin,
                riskSeg,
                email == null ? "" : email,
                phone == null ? "" : phone,
                addr
        );
    }

    public static PatchCustomerUseCase.Command toPatchCustomerCommand(PatchCustomerRequest req) {

        UUID id = requireUuid(req.getId(), "id");

        String fullName = req.hasFullName() ? optionalTrim(req.getFullName(), "full_name", 120) : null;

        // enums “nullable”
        String riskSegment = null;
        if (req.hasRiskSegment()) {
            riskSegment = ProtoEnumMapper.toDbRiskSegmentOrNull(req.getRiskSegment().getValue());
        }

        String customerStatus = null;
        if (req.hasCustomerStatus()) {
            customerStatus = ProtoEnumMapper.toDbCustomerStatusOrNull(req.getCustomerStatus().getValue());
        }

        PatchCustomerUseCase.ContactPatch contact = null;
        if (req.hasContact()) {
            var c = req.getContact();
            String email = c.hasEmail() ? optionalEmail(c.getEmail().getValue(), "contact.email") : null;
            String phone = c.hasPhone() ? optionalPhone(c.getPhone().getValue(), "contact.phone") : null;
            contact = new PatchCustomerUseCase.ContactPatch(email, phone);
        }

        PatchCustomerUseCase.PreferencesPatch prefs = null;
        if (req.hasPreferences()) {
            var p = req.getPreferences();
            String channel = null;
            if (p.hasChannel()) {
                var v = p.getChannel().getValue();
                channel = switch (v) {
                    case PREFERENCE_CHANNEL_EMAIL -> "email";
                    case PREFERENCE_CHANNEL_SMS -> "sms";
                    case PREFERENCE_CHANNEL_PUSH -> "push";
                    case PREFERENCE_CHANNEL_UNSPECIFIED, UNRECOGNIZED -> null;
                };
            }
            Boolean optIn = p.hasOptIn() ? p.getOptIn().getValue() : null;
            prefs = new PatchCustomerUseCase.PreferencesPatch(channel, optIn);
        }

        // “no-op patch” (opcional): si nada viene, rechaza
        boolean hasAny = fullName != null || riskSegment != null || customerStatus != null || contact != null || prefs != null;
        if (!hasAny) throw invalid("No fields to patch");

        return new PatchCustomerUseCase.Command(id, fullName, riskSegment, customerStatus, contact, prefs);
    }
}