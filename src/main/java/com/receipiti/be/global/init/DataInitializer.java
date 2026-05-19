package com.receipiti.be.global.init;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.enums.CategoryType;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.countByMemberIsNull() == 0) {
            Arrays.stream(CategoryType.values())
                    .forEach(type -> categoryRepository.save(
                            Category.builder()
                                    .categoryType(type)
                                    .name(type.getDescription())
                                    .build()
                    ));
        }
    }
}