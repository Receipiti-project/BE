package com.receipiti.be.domain.store.repository;

import com.receipiti.be.domain.store.entity.Store;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByName(String name);

    Optional<Store> findByKakaoPlaceId(String kakaoPlaceId);

    @Modifying
    @Query(value = """
            INSERT INTO store (
                store_name, kakao_place_id, road_address, biz_category,
                latitude, longitude, created_at, updated_at
            )
            VALUES (
                :name, :placeId, :address, :businessCategory,
                :latitude, :longitude, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE kakao_place_id = VALUES(kakao_place_id)
            """, nativeQuery = true)
    void insertIfAbsentByKakaoPlaceId(
            @Param("name") String name,
            @Param("placeId") String placeId,
            @Param("address") String address,
            @Param("businessCategory") String businessCategory,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude
    );
}
