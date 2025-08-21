package com.santiago_tumbaco.identity.domain.dto;

public record PersonDto(
        Long id,
        String name,
        String lastName,
        String email
) {}
