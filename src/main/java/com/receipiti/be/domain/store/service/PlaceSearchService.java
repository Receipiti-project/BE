package com.receipiti.be.domain.store.service;

import com.receipiti.be.domain.store.dto.KakaoPlaceSearchResponse;
import com.receipiti.be.domain.store.dto.PlaceSearchResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private static final String KAKAO_LOCAL_HOST = "dapi.kakao.com";
    private static final String KAKAO_LOCAL_SEARCH_PATH = "/v2/local/search/keyword.json";
    private static final int RESULT_SIZE = 15;

    private final RestClient kakaoRestClient;

    @Value("${kakao.local.rest-api-key}")
    private String restApiKey;

    public List<PlaceSearchResponse> search(
            String query,
            BigDecimal latitude,
            BigDecimal longitude,
            int radius
    ) {
        try {
            KakaoPlaceSearchResponse response = kakaoRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host(KAKAO_LOCAL_HOST)
                            .path(KAKAO_LOCAL_SEARCH_PATH)
                            .queryParam("query", query.trim())
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", radius)
                            .queryParam("sort", "distance")
                            .queryParam("size", RESULT_SIZE)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(KakaoPlaceSearchResponse.class);

            if (response == null || response.documents() == null) {
                throw new GeneralException(GeneralErrorCode.PLACE_SEARCH_SERVER_ERROR);
            }

            return response.documents().stream()
                    .map(PlaceSearchResponse::from)
                    .toList();
        } catch (RestClientResponseException exception) {
            throw new GeneralException(GeneralErrorCode.PLACE_SEARCH_SERVER_ERROR);
        } catch (RestClientException exception) {
            throw new GeneralException(GeneralErrorCode.PLACE_SEARCH_SERVICE_UNAVAILABLE);
        }
    }
}
