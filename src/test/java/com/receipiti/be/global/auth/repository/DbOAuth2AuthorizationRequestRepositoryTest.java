package com.receipiti.be.global.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.receipiti.be.global.auth.entity.OAuth2AuthorizationRequestState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DbOAuth2AuthorizationRequestRepositoryTest {

    @Mock
    private OAuth2AuthorizationRequestStateRepository stateRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private DbOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @BeforeEach
    void setUp() {
        authorizationRequestRepository = new DbOAuth2AuthorizationRequestRepository(stateRepository);
        ReflectionTestUtils.setField(authorizationRequestRepository, "expirationSeconds", 300L);
    }

    @Test
    void 세션_없이_state로_인가_요청을_저장하고_한_번만_복원한다() {
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequest();
        AtomicReference<OAuth2AuthorizationRequestState> savedState = new AtomicReference<>();
        when(stateRepository.save(any(OAuth2AuthorizationRequestState.class)))
                .thenAnswer(invocation -> {
                    OAuth2AuthorizationRequestState state = invocation.getArgument(0);
                    savedState.set(state);
                    return state;
                });

        authorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, request, response);

        when(request.getParameter("state")).thenReturn("oauth-state");
        when(stateRepository.findByStateForUpdate("oauth-state"))
                .thenReturn(Optional.of(savedState.get()));

        OAuth2AuthorizationRequest restored = authorizationRequestRepository
                .removeAuthorizationRequest(request, response);

        assertThat(restored).isNotNull();
        assertThat(restored.getState()).isEqualTo("oauth-state");
        assertThat(restored.getClientId()).isEqualTo("client-id");
        assertThat(restored.getRedirectUri()).isEqualTo("https://api.example.com/login/oauth2/code/kakao");
        verify(stateRepository).delete(savedState.get());
    }

    @Test
    void 만료된_인가_요청은_복원하지_않는다() {
        ReflectionTestUtils.setField(authorizationRequestRepository, "expirationSeconds", -1L);
        AtomicReference<OAuth2AuthorizationRequestState> savedState = new AtomicReference<>();
        when(stateRepository.save(any(OAuth2AuthorizationRequestState.class)))
                .thenAnswer(invocation -> {
                    OAuth2AuthorizationRequestState state = invocation.getArgument(0);
                    savedState.set(state);
                    return state;
                });
        authorizationRequestRepository.saveAuthorizationRequest(authorizationRequest(), request, response);
        when(request.getParameter("state")).thenReturn("oauth-state");
        when(stateRepository.findByStateForUpdate("oauth-state"))
                .thenReturn(Optional.of(savedState.get()));

        OAuth2AuthorizationRequest restored = authorizationRequestRepository
                .removeAuthorizationRequest(request, response);

        assertThat(restored).isNull();
        verify(stateRepository).delete(savedState.get());
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("client-id")
                .redirectUri("https://api.example.com/login/oauth2/code/kakao")
                .scopes(Set.of("profile_nickname", "account_email"))
                .state("oauth-state")
                .authorizationRequestUri("https://kauth.kakao.com/oauth/authorize?state=oauth-state")
                .build();
    }
}
