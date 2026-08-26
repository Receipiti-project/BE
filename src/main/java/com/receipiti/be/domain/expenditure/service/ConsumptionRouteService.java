package com.receipiti.be.domain.expenditure.service;

import com.receipiti.be.domain.expenditure.dto.response.ConsumptionRoutePlaceResponse;
import com.receipiti.be.domain.expenditure.dto.response.ConsumptionRouteResponse;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsumptionRouteService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final ExpenditureRepository expenditureRepository;

    public ConsumptionRouteResponse getRoute(Member member, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Expenditure> expenditures = expenditureRepository.findRouteByDate(
                member,
                start,
                end
        );

        long totalExpenditure = expenditures.stream()
                .mapToLong(Expenditure::getAmount)
                .sum();
        List<Expenditure> mappedExpenditures = expenditures.stream()
                .filter(this::hasCoordinates)
                .toList();

        List<ConsumptionRoutePlaceResponse> places = java.util.stream.IntStream
                .range(0, mappedExpenditures.size())
                .mapToObj(index -> ConsumptionRoutePlaceResponse.from(
                        index + 1,
                        mappedExpenditures.get(index)
                ))
                .toList();

        return new ConsumptionRouteResponse(
                date,
                places.size(),
                expenditures.size() - places.size(),
                totalExpenditure,
                calculateDistanceMeters(mappedExpenditures),
                places
        );
    }

    private boolean hasCoordinates(Expenditure expenditure) {
        Store store = expenditure.getStore();
        return store.getLatitude() != null && store.getLongitude() != null;
    }

    private long calculateDistanceMeters(List<Expenditure> expenditures) {
        long distanceMeters = 0L;
        for (int index = 1; index < expenditures.size(); index++) {
            distanceMeters += distanceBetween(
                    expenditures.get(index - 1).getStore(),
                    expenditures.get(index).getStore()
            );
        }
        return distanceMeters;
    }

    private long distanceBetween(Store origin, Store destination) {
        double originLatitude = Math.toRadians(origin.getLatitude().doubleValue());
        double destinationLatitude = Math.toRadians(destination.getLatitude().doubleValue());
        double latitudeDifference = destinationLatitude - originLatitude;
        double longitudeDifference = Math.toRadians(
                destination.getLongitude().doubleValue()
                        - origin.getLongitude().doubleValue()
        );

        double haversine = Math.pow(Math.sin(latitudeDifference / 2), 2)
                + Math.cos(originLatitude)
                * Math.cos(destinationLatitude)
                * Math.pow(Math.sin(longitudeDifference / 2), 2);
        haversine = Math.min(1.0, haversine);
        double angularDistance = 2 * Math.atan2(
                Math.sqrt(haversine),
                Math.sqrt(1 - haversine)
        );
        return Math.round(EARTH_RADIUS_METERS * angularDistance);
    }
}
