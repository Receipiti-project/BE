package com.receipiti.be.domain.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.receipiti.be.domain.store.dto.PlaceSearchResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PlaceSearchServiceTest {

    private MockRestServiceServer server;
    private PlaceSearchService placeSearchService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        placeSearchService = new PlaceSearchService(builder.build());
        ReflectionTestUtils.setField(placeSearchService, "restApiKey", "test-key");
    }

    @Test
    void 사용자_위치에서_가까운_장소를_검색한다() {
        server.expect(requestTo("https://dapi.kakao.com/v2/local/search/keyword.json"
                        + "?query=%EC%8A%A4%ED%83%80%EB%B2%85%EC%8A%A4"
                        + "&x=126.9236&y=37.5561&radius=5000&sort=distance&size=15"))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {
                              "id": "123",
                              "place_name": "스타벅스 홍대점",
                              "category_name": "음식점 > 카페 > 커피전문점",
                              "phone": "02-1234-5678",
                              "address_name": "서울 마포구 동교동",
                              "road_address_name": "서울 마포구 홍익로 1",
                              "x": "126.9236",
                              "y": "37.5561",
                              "place_url": "https://place.map.kakao.com/123",
                              "distance": "120"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<PlaceSearchResponse> result = placeSearchService.search(
                "스타벅스",
                new BigDecimal("37.5561"),
                new BigDecimal("126.9236"),
                5000
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().placeId()).isEqualTo("123");
        assertThat(result.getFirst().latitude()).isEqualByComparingTo("37.5561");
        assertThat(result.getFirst().longitude()).isEqualByComparingTo("126.9236");
        assertThat(result.getFirst().distanceMeters()).isEqualTo(120L);
        server.verify();
    }

    @Test
    void 카카오_장소_검색_서버가_실패하면_외부_서버_오류를_반환한다() {
        server.expect(requestTo(org.hamcrest.Matchers.any(String.class)))
                .andRespond(withServerError());

        assertThatThrownBy(() -> placeSearchService.search(
                "스타벅스",
                new BigDecimal("37.5561"),
                new BigDecimal("126.9236"),
                5000
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.PLACE_SEARCH_SERVER_ERROR);
    }
}
