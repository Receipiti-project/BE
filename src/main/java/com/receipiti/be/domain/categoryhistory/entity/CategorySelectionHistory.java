package com.receipiti.be.domain.categoryhistory.entity;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.categoryhistory.enums.CategorySelectionSource;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "category_selection_history",
        indexes = {
                @Index(name = "idx_category_history_member_store", columnList = "member_id, store_id"),
                @Index(name = "idx_category_history_member_brand", columnList = "member_id, normalized_brand_name"),
                @Index(name = "idx_category_history_member_business", columnList = "member_id, business_category"),
                @Index(name = "idx_category_history_member_created", columnList = "member_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CategorySelectionHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_selection_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "store_name", nullable = false, length = 50)
    private String storeName;

    @Column(name = "normalized_store_name", nullable = false, length = 50)
    private String normalizedStoreName;

    @Column(name = "normalized_brand_name", length = 50)
    private String normalizedBrandName;

    @Column(name = "business_category", length = 100)
    private String businessCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_source", nullable = false, length = 20)
    private CategorySelectionSource selectionSource;
}
