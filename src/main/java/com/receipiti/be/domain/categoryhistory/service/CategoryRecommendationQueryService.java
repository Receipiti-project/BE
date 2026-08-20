package com.receipiti.be.domain.categoryhistory.service;

import com.receipiti.be.domain.categoryhistory.dto.CategoryRecommendationResponse;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryRecommendationQueryService {

    private final StoreRepository storeRepository;
    private final PersonalizedCategoryService personalizedCategoryService;

    public CategoryRecommendationResponse getRecommendation(
            Member member,
            String storeName,
            String businessCategory
    ) {
        String trimmedStoreName = storeName.trim();
        Store store = storeRepository.findByName(trimmedStoreName)
                .orElseGet(() -> Store.builder()
                        .name(trimmedStoreName)
                        .bizCategory(trimToNull(businessCategory))
                        .build());

        return CategoryRecommendationResponse.from(
                personalizedCategoryService.recommend(member, store, trimToNull(businessCategory))
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
