package com.receipiti.be.global.auth.repository;

import com.receipiti.be.global.auth.entity.OAuth2AuthorizationRequestState;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuth2AuthorizationRequestStateRepository
        extends JpaRepository<OAuth2AuthorizationRequestState, String> {

    void deleteByExpiresAtBefore(LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OAuth2AuthorizationRequestState r WHERE r.state = :state")
    Optional<OAuth2AuthorizationRequestState> findByStateForUpdate(@Param("state") String state);
}
