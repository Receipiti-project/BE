package com.receipiti.be.domain.store.entity;

import com.receipiti.be.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Store extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(name="store_name", nullable = false, length = 50)
    private String name;

    @Column(name = "kakao_place_id", unique = true, length = 50)
    private String kakaoPlaceId;

    @Column(name="road_address")
    private String address;

    @Column(name="biz_category", length = 100)
    private String bizCategory;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 7)
    private BigDecimal longitude;

    public void fillBusinessCategoryIfAbsent(String businessCategory) {
        if ((this.bizCategory == null || this.bizCategory.isBlank())
                && businessCategory != null
                && !businessCategory.isBlank()) {
            this.bizCategory = businessCategory.trim();
        }
    }

    public void fillLocationIfAbsent(
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if ((this.address == null || this.address.isBlank())
                && address != null
                && !address.isBlank()) {
            this.address = address.trim();
        }
        if (this.latitude == null && latitude != null) {
            this.latitude = latitude;
        }
        if (this.longitude == null && longitude != null) {
            this.longitude = longitude;
        }
    }

    public void updateLocation(
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (address != null && !address.isBlank()) {
            this.address = address.trim();
        }
        if (latitude != null) {
            this.latitude = latitude;
        }
        if (longitude != null) {
            this.longitude = longitude;
        }
    }

}
