package com.receipiti.be.domain.store.dto;

import java.math.BigDecimal;

public record PlaceSearchResponse(
        String placeId,
        String placeName,
        String categoryName,
        String phone,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl,
        Long distanceMeters
) {

    public static PlaceSearchResponse from(KakaoPlaceSearchResponse.Document document) {
        return new PlaceSearchResponse(
                document.id(),
                document.placeName(),
                document.categoryName(),
                document.phone(),
                document.addressName(),
                document.roadAddressName(),
                new BigDecimal(document.y()),
                new BigDecimal(document.x()),
                document.placeUrl(),
                parseDistance(document.distance())
        );
    }

    private static Long parseDistance(String distance) {
        if (distance == null || distance.isBlank()) {
            return null;
        }
        return Long.valueOf(distance);
    }
}
