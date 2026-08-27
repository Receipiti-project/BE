package com.receipiti.be.domain.member.repository;

import com.receipiti.be.domain.member.entity.LoginCode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginCodeRepository extends JpaRepository<LoginCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM LoginCode c JOIN FETCH c.member WHERE c.codeHash = :codeHash")
    Optional<LoginCode> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
