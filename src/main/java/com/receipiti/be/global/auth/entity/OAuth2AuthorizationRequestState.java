package com.receipiti.be.global.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "oauth2_authorization_request",
        indexes = @Index(name = "idx_oauth2_authorization_request_expires_at", columnList = "expires_at")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuth2AuthorizationRequestState {

    @Id
    @Column(name = "state", length = 100)
    private String state;

    @Lob
    @Column(name = "request_payload", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String requestPayload;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpiredAt(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
