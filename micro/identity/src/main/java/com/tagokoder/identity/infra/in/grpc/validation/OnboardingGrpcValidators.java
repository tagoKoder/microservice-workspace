package com.tagokoder.identity.infra.in.grpc.validation;

import bank.identity.v1.*;
import com.tagokoder.identity.domain.model.kyc.KycDocumentKind;
import com.tagokoder.identity.domain.port.in.ActivateRegistrationUseCase;
import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;

import java.util.*;

import static com.tagokoder.identity.infra.in.grpc.validation.GrpcValidation.*;

public final class OnboardingGrpcValidators {
  private OnboardingGrpcValidators() {}

  public static StartRegistrationUseCase.StartRegistrationCommand toStartRegistrationCommand(StartRegistrationRequest req) {

    String channel = requireChannel(req.getChannel());

    String nationalId = requireNationalId(req.getNationalId());
    var issueDate = requireIsoDate(req.getNationalIdIssueDate(), "national_id_issue_date");

    String fingerprint = requireFingerprintCode(req.getFingerprintCode());

    var monthlyIncome = requireNonNegativeMoney(req.getMonthlyIncome(), "monthly_income");

    // enum requerido
    if (req.getOccupationType() == OccupationType.OCCUPATION_TYPE_UNSPECIFIED || req.getOccupationType() == OccupationType.UNRECOGNIZED) {
      throw invalid("occupation_type is required");
    }
    String occupation = req.getOccupationType().name();

    String email = optionalEmail(req.getEmail(), "email");
    String phone = optionalPhone(req.getPhone(), "phone");
    if (email == null && phone == null) throw invalid("email or phone is required");

    // hints de content-type (no obligatorios)
    String idFrontCt = optionalMimeType(req.getIdFrontContentType(), "id_front_content_type");
    String selfieCt  = optionalMimeType(req.getSelfieContentType(), "selfie_content_type");

    return new StartRegistrationUseCase.StartRegistrationCommand(
        channel,
        nationalId,
        issueDate,
        fingerprint,
        monthlyIncome,
        occupation,
        email == null ? "" : email,
        phone == null ? "" : phone,
        idFrontCt,
        selfieCt
    );
  }

  public record ConfirmKycIn(UUID registrationId, java.util.List<com.tagokoder.identity.domain.model.kyc.UploadedObject> objects, String channel) {}

  public static ConfirmKycIn toConfirmKycInput(ConfirmRegistrationKycRequest req) {
    UUID regId = requireUuid(req.getRegistrationId(), "registration_id");
    String channel = optionalTrim(req.getChannel(), "channel", 16);
    if (channel != null) {
      // reusa el mismo criterio de channel
      channel = requireChannel(channel);
    } else {
      channel = "";
    }

    int n = req.getObjectsCount();
    if (n <= 0) throw invalid("objects is required");
    if (n > 5) throw invalid("max 5 objects"); // DoS guard (aunque en tu diseño son 2)

    // evita duplicados por doc_type
    Set<KycDocType> seen = new HashSet<>();
    var out = new ArrayList<com.tagokoder.identity.domain.model.kyc.UploadedObject>(n);

    for (var o : req.getObjectsList()) {
      if (o.getDocType() == KycDocType.KYC_DOC_TYPE_UNSPECIFIED || o.getDocType() == KycDocType.UNRECOGNIZED) {
        throw invalid("objects[].doc_type is required");
      }
      if (!seen.add(o.getDocType())) throw invalid("duplicate doc_type in objects");

      String bucket = requireS3Bucket(o.getBucket(), "objects[].bucket");
      String key = requireS3Key(o.getKey(), "objects[].key");
      String etag = optionalEtag(o.getEtag(), "objects[].etag");

      Long sizeBytes = null;
      if (o.getSizeBytes() > 0) {
        if (o.getSizeBytes() > 20_000_000L) throw invalid("objects[].size_bytes too large"); // guard demo
        sizeBytes = o.getSizeBytes();
      }

      String contentType = optionalMimeType(o.getContentType(), "objects[].content_type");

      // map doc type a dominio
      KycDocumentKind kind = switch (o.getDocType()) {
        case KYC_DOC_TYPE_ID_FRONT -> KycDocumentKind.ID_FRONT;
        case KYC_DOC_TYPE_SELFIE -> KycDocumentKind.SELFIE;
        default -> throw invalid("objects[].doc_type invalid");
      };

      out.add(new com.tagokoder.identity.domain.model.kyc.UploadedObject(
          kind,
          bucket,
          key,
          etag,
          sizeBytes,
          contentType
      ));
    }

    // opcional: exige exactamente los 2 docs para activar
    // if (!seen.contains(KycDocType.KYC_DOC_TYPE_ID_FRONT) || !seen.contains(KycDocType.KYC_DOC_TYPE_SELFIE)) {
    //   throw invalid("both ID_FRONT and SELFIE are required");
    // }

    return new ConfirmKycIn(regId, out, channel);
  }

  public static ActivateRegistrationUseCase.ActivateRegistrationCommand toActivateCommand(ActivateRegistrationRequest req, String corrId) {

    UUID regId = requireUuid(req.getRegistrationId(), "registration_id");
    String channel = requireChannel(req.getChannel());

    String fullName = requireNonBlank(req.getFullName(), "full_name");
    if (fullName.length() > 120) throw invalid("full_name too long");

    String tin = requireNonBlank(req.getTin(), "tin");
    if (tin.length() > 32) throw invalid("tin too long");
    if (!tin.matches("^[A-Za-z0-9\\-]+$")) throw invalid("tin invalid");

    var birthDate = requireIsoDate(req.getBirthDate(), "birth_date");
    String country = requireCountry2(req.getCountry(), "country");

    String email = optionalEmail(req.getEmail(), "email");
    String phone = optionalPhone(req.getPhone(), "phone");
    if (email == null && phone == null) throw invalid("email or phone is required");

    requireAcceptedTerms(req.getAcceptedTerms());

    String correlationId = optionalTrim(corrId, "correlation_id", 128);
    if (correlationId == null) correlationId = "";

    return new ActivateRegistrationUseCase.ActivateRegistrationCommand(
        regId,
        channel,
        fullName,
        tin,
        birthDate,
        country,
        email == null ? "" : email,
        phone == null ? "" : phone,
        true,
        correlationId
    );
  }
}