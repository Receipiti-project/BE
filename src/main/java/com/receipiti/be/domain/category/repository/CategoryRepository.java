package com.receipiti.be.domain.category.repository;

import com.receipiti.be.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    long countByMemberIsNull();
}
