package com.receipiti.be.domain.expenditure.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.receipiti.be.domain.expenditure.enums.Currency;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ExpenditureUpdateRequest {
    private Long categoryId;
    @Size(max = 50, message = "상호명은 50자 이하로 입력해주세요.")
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
    @PositiveOrZero(message = "지출 금액은 0원 이상이어야 합니다.")
    private Long amount;
    private LocalDateTime expenditureDate;
    @Size(max = 255, message = "메모는 255자 이하로 입력해주세요.")
    private String memo;
    private Currency currency;

    @JsonIgnore
    @AssertTrue(message = "장소를 수정하는 경우 placeId, latitude, longitude를 모두 입력해야 합니다.")
    public boolean isPlaceSelectionValid() {
        boolean hasPlaceId = placeId != null && !placeId.isBlank();
        boolean hasAddress = address != null && !address.isBlank();
        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;
        boolean hasNoPlaceSelection = !hasPlaceId && !hasAddress && !hasLatitude && !hasLongitude;
        boolean hasCompletePlaceSelection = hasPlaceId && hasLatitude && hasLongitude;
        return hasNoPlaceSelection || hasCompletePlaceSelection;
    }
}
