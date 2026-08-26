package com.receipiti.be.domain.expenditure.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ConsumptionRouteResponse(
        LocalDate date,
        int visitedPlaceCount,
        int unmappedExpenditureCount,
        long totalExpenditure,
        long estimatedDistanceMeters,
        List<ConsumptionRoutePlaceResponse> places
) {
}
