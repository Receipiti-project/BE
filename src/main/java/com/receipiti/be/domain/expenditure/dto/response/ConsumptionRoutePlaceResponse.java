package com.receipiti.be.domain.expenditure.dto.response;

import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.store.entity.Store;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsumptionRoutePlaceResponse(
        int sequence,
        Long expenditureId,
        String placeId,
        String storeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String categoryName,
        Long amount,
        LocalDateTime visitedAt
) {

    public static ConsumptionRoutePlaceResponse from(int sequence, Expenditure expenditure) {
        Store store = expenditure.getStore();
        return new ConsumptionRoutePlaceResponse(
                sequence,
                expenditure.getId(),
                store.getKakaoPlaceId(),
                store.getName(),
                store.getAddress(),
                store.getLatitude(),
                store.getLongitude(),
                expenditure.getCategory().getName(),
                expenditure.getAmount(),
                expenditure.getExpenditureDate()
        );
    }
}
