package com.receipiti.be.domain.categoryhistory.repository;

import com.receipiti.be.domain.categoryhistory.entity.CategorySelectionHistory;
import com.receipiti.be.domain.member.entity.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorySelectionHistoryRepository extends JpaRepository<CategorySelectionHistory, Long> {

    List<CategorySelectionHistory> findAllByMemberOrderByCreatedAtDesc(Member member);
}
