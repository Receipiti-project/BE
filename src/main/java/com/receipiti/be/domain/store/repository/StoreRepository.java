package com.receipiti.be.domain.store.repository;

import com.receipiti.be.domain.store.entity.Store;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByName(String name);
}
