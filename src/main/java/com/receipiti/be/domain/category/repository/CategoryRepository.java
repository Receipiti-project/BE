package com.receipiti.be.domain.category.repository;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    long countByMemberIsNull();

    List<Category> findByMemberIsNullOrMember(Member member);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM Category c
            WHERE c.id = :id
              AND (c.member IS NULL OR c.member = :member)
            """)
    Optional<Category> findAccessibleCategoryForUpdate(
            @Param("id") Long id,
            @Param("member") Member member);
}
