package com.santiago_tumbaco.identity.domain.dto;

public record WhoAmIResult(
        Long accountId,
        PersonDto person
) {}