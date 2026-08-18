package com.receipiti.be.global.init;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.enums.CategoryType;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final ExpenditureRepository expenditureRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Arrays.stream(CategoryType.values())
                .filter(type -> type != CategoryType.CUSTOM)
                .filter(type -> !categoryRepository.existsByMemberIsNullAndCategoryType(type))
                .forEach(type -> categoryRepository.save(
                        Category.builder()
                                .categoryType(type)
                                .name(type.getDescription())
                                .build()
                ));

        migrateGlobalCustomCategories();
    }

    private void migrateGlobalCustomCategories() {
        Category defaultEtc = categoryRepository
                .findFirstByMemberIsNullAndCategoryType(CategoryType.ETC)
                .orElseThrow(() -> new IllegalStateException("기본 기타 카테고리 초기화에 실패했습니다."));

        categoryRepository.findAllByMemberIsNullAndCategoryType(CategoryType.CUSTOM)
                .forEach(globalCustom -> {
                    expenditureRepository.replaceCategory(globalCustom, defaultEtc);
                    categoryRepository.delete(globalCustom);
                });
    }
}
