package com.receipiti.be.domain.categoryhistory.service;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.categoryhistory.entity.CategorySelectionHistory;
import com.receipiti.be.domain.categoryhistory.enums.CategorySelectionSource;
import com.receipiti.be.domain.categoryhistory.repository.CategorySelectionHistoryRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategorySelectionHistoryService {

    private final CategorySelectionHistoryRepository categorySelectionHistoryRepository;
    private final MerchantNameNormalizer merchantNameNormalizer;

    public void recordSelection(
            Member member,
            Category category,
            Store store,
            CategorySelectionSource selectionSource
    ) {
        CategorySelectionHistory history = CategorySelectionHistory.builder()
                .member(member)
                .category(category)
                .store(store)
                .storeName(store.getName())
                .normalizedStoreName(merchantNameNormalizer.normalizeStoreName(store.getName()))
                .normalizedBrandName(merchantNameNormalizer.normalizeBrandName(store.getName()))
                .businessCategory(store.getBizCategory())
                .selectionSource(selectionSource)
                .build();

        categorySelectionHistoryRepository.save(history);
    }
}
