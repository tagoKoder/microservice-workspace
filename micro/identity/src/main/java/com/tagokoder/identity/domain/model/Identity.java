package com.tagokoder.identity.domain.model;


import java.time.Instant;
import java.util.UUID;

public class Identity {

    private final UUID id;
    private final String subjectIdOidc;
    private final String provider;
    private final UserStatus userStatus;
    private final Instant createdAt;

    public enum UserStatus {
        ACTIVE, LOCKED, DISABLED
    }

    public Identity(UUID id, String subjectIdOidc, String provider,
                    UserStatus userStatus, Instant createdAt) {
        this.id = id;
        this.subjectIdOidc = subjectIdOidc;
        this.provider = provider;
        this.userStatus = userStatus;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getSubjectIdOidc() { return subjectIdOidc; }
    public String getProvider() { return provider; }
    public UserStatus getUserStatus() { return userStatus; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isActive() {
        return userStatus == UserStatus.ACTIVE;
    }

    public Identity activate() {
        return new Identity(id, subjectIdOidc, provider, UserStatus.ACTIVE, createdAt);
    }
}