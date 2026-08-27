package com.receipiti.be.global.auth.repository;

import com.receipiti.be.global.auth.entity.OAuth2AuthorizationRequestState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DbOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String STATE_PARAMETER = "state";

    private final OAuth2AuthorizationRequestStateRepository stateRepository;

    @Value("${oauth2.authorization-request-expiration-seconds:300}")
    private long expirationSeconds;

    @Override
    @Transactional(readOnly = true)
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(STATE_PARAMETER);
        if (state == null || state.isBlank()) {
            return null;
        }

        return stateRepository.findById(state)
                .filter(savedRequest -> !savedRequest.isExpiredAt(LocalDateTime.now()))
                .map(savedRequest -> deserialize(savedRequest.getRequestPayload()))
                .orElse(null);
    }

    @Override
    @Transactional
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            removeByRequestState(request);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        stateRepository.deleteByExpiresAtBefore(now);
        stateRepository.save(OAuth2AuthorizationRequestState.builder()
                .state(authorizationRequest.getState())
                .requestPayload(serialize(authorizationRequest))
                .expiresAt(now.plusSeconds(expirationSeconds))
                .build());
    }

    @Override
    @Transactional
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String state = request.getParameter(STATE_PARAMETER);
        if (state == null || state.isBlank()) {
            return null;
        }

        return stateRepository.findByStateForUpdate(state)
                .map(savedRequest -> {
                    stateRepository.delete(savedRequest);
                    if (savedRequest.isExpiredAt(LocalDateTime.now())) {
                        return null;
                    }
                    return deserialize(savedRequest.getRequestPayload());
                })
                .orElse(null);
    }

    private void removeByRequestState(HttpServletRequest request) {
        String state = request.getParameter(STATE_PARAMETER);
        if (state != null && !state.isBlank()) {
            stateRepository.deleteById(state);
        }
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(authorizationRequest);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("OAuth2 인가 요청을 저장할 수 없습니다.", exception);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String payload) {
        byte[] bytes = Base64.getDecoder().decode(payload);
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (OAuth2AuthorizationRequest) input.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("OAuth2 인가 요청을 복원할 수 없습니다.", exception);
        }
    }
}
