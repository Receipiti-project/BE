package com.receipiti.be.domain.categoryhistory.repository;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.categoryhistory.entity.CategorySelectionHistory;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorySelectionHistoryRepository extends JpaRepository<CategorySelectionHistory, Long> {

    List<CategorySelectionHistory> findAllByMemberOrderByCreatedAtDesc(Member member);

    long deleteAllByMemberAndCategory(Member member, Category category);

    @EntityGraph(attributePaths = "category")
    List<CategorySelectionHistory> findAllByMemberAndStoreOrderByCreatedAtDesc(
            Member member,
            Store store
    );

    @EntityGraph(attributePaths = "category")
    List<CategorySelectionHistory> findAllByMemberAndNormalizedBrandNameOrderByCreatedAtDesc(
            Member member,
            String normalizedBrandName
    );

    @EntityGraph(attributePaths = "category")
    List<CategorySelectionHistory> findAllByMemberAndBusinessCategoryOrderByCreatedAtDesc(
            Member member,
            String businessCategory
    );
}
