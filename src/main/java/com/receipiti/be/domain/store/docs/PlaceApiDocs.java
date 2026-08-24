package com.receipiti.be.domain.store.docs;

import com.receipiti.be.domain.store.dto.PlaceSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Place", description = "장소 검색 관련 API")
public interface PlaceApiDocs {

    @Operation(
            summary = "주변 장소 검색",
            description = "가맹점명과 사용자 위치를 기준으로 가까운 카카오 장소 후보를 조회합니다."
    )
    @Parameters({
            @Parameter(name = "query", description = "검색할 가맹점명", example = "스타벅스"),
            @Parameter(name = "latitude", description = "사용자 현재 위도", example = "37.5561"),
            @Parameter(name = "longitude", description = "사용자 현재 경도", example = "126.9236"),
            @Parameter(name = "radius", description = "검색 반경(m), 기본값 5000", example = "5000")
    })
    ResponseEntity<List<PlaceSearchResponse>> searchPlaces(
            @NotBlank String query,
            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @Min(0) @Max(20000) int radius
    );
}
