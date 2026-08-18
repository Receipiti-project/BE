package com.receipiti.be.domain.category.repository;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.member.entity.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    long countByMemberIsNull();

    List<Category> findByMemberIsNullOrMember(Member member);

    Optional<Category> findByIdAndMember(Long id, Member member);
}
