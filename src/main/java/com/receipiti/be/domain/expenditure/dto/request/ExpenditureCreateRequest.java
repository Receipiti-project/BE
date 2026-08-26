package com.receipiti.be.domain.expenditure.dto.request;

import com.receipiti.be.domain.expenditure.enums.Currency;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExpenditureCreateRequest {
    private Long categoryId;
    private Long defaultCategoryId;
    @NotBlank
    private String storeName;
    @Size(max = 50, message = "카카오 장소 ID는 50자 이하로 입력해주세요.")
    private String placeId;
    @Size(max = 255, message = "가맹점 주소는 255자 이하로 입력해주세요.")
    private String address;
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
    private BigDecimal latitude;
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
    private BigDecimal longitude;
    @Size(max = 100, message = "업종명은 100자 이하로 입력해주세요.")
    private String businessCategory;
    @NotNull
    @Positive
    private Long amount;
    @NotNull
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;

    @JsonIgnore
    @AssertTrue(message = "categoryId 또는 defaultCategoryId 중 하나는 필수입니다.")
    public boolean isCategorySelectionValid() {
        return categoryId != null || defaultCategoryId != null;
    }

    @JsonIgnore
    @AssertTrue(message = "장소를 선택한 경우 placeId, latitude, longitude를 모두 입력해야 합니다.")
    public boolean isPlaceSelectionValid() {
        boolean hasPlaceId = placeId != null && !placeId.isBlank();
        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;
        boolean hasNoPlaceSelection = !hasPlaceId && !hasLatitude && !hasLongitude;
        boolean hasCompletePlaceSelection = hasPlaceId && hasLatitude && hasLongitude;
        return hasNoPlaceSelection || hasCompletePlaceSelection;
    }
}
