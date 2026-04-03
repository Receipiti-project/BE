package com.receipiti.be.domain.expenditure.entity;

import com.receipiti.be.domain.expenditure.enums.Currency;
import com.receipiti.be.domain.expenditure.enums.InputType;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "expenditure")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Expenditure extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expenditure_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "category_id", nullable = false)
//    private Category category;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "store_id", nullable = false)
//    private Store store;

    @Column(nullable = false)
    private Long amount;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InputType inputType;

    private String memo;

    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime expenditureDate;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
