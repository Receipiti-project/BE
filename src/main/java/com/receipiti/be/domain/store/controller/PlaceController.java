package com.receipiti.be.domain.store.controller;

import com.receipiti.be.domain.store.docs.PlaceApiDocs;
import com.receipiti.be.domain.store.dto.PlaceSearchResponse;
import com.receipiti.be.domain.store.service.PlaceSearchService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
public class PlaceController implements PlaceApiDocs {

    private final PlaceSearchService placeSearchService;

    @Override
    @GetMapping("/search")
    public ResponseEntity<List<PlaceSearchResponse>> searchPlaces(
            @RequestParam String query,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "5000") int radius
    ) {
        return ResponseEntity.ok(placeSearchService.search(
                query,
                latitude,
                longitude,
                radius
        ));
    }
}
