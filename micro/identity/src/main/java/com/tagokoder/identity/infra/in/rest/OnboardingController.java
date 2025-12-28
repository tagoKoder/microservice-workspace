package com.tagokoder.identity.infra.in.rest;

import java.time.ZoneOffset;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tagokoder.identity.domain.port.in.StartRegistrationUseCase;
import com.tagokoder.identity.infra.in.api.model.RegistrationStartRequestDto;
import com.tagokoder.identity.infra.in.api.model.RegistrationStartResponseDto;

@Controller
@Validated
@RequestMapping("/api/v1")
public class OnboardingController {

    private final StartRegistrationUseCase startRegistration;

    public OnboardingController(StartRegistrationUseCase startRegistration) {
        this.startRegistration = startRegistration;
    }

    @PostMapping("/identity/onboarding/registrations")
    public ResponseEntity<RegistrationStartResponseDto> startRegistration(
            @RequestBody RegistrationStartRequestDto body
    ) {

        // Convertimos types del DTO a los tipos del caso de uso
        Double monthlyIncome = body.getMonthlyIncome();
        String occupationType = body.getOccupationType() != null
                ? body.getOccupationType().getValue()  // enum -> String
                : null;

        var res = startRegistration.start(
                new StartRegistrationUseCase.StartRegistrationCommand(
                        body.getChannel(),
                        body.getNationalId(),
                        body.getNationalIdIssueDate(),
                        body.getFingerprintCode(),
                        body.getIdDocumentFrontBase64(),
                        body.getSelfieBase64(),
                        monthlyIncome != null ? monthlyIncome : 0d,
                        occupationType,
                        body.getEmail(),
                        body.getPhone()
                )
        );

        var dto = new RegistrationStartResponseDto();
        dto.setRegistrationId(res.registrationId());
        dto.setState(res.state());
        // Instant -> OffsetDateTime (UTC)
        dto.setCreatedAt(res.createdAt().atOffset(ZoneOffset.UTC));

        return ResponseEntity.status(201).body(dto);
    }
}
