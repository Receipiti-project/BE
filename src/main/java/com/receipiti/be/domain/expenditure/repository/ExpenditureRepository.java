package com.receipiti.be.domain.expenditure.repository;

import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.member.entity.Member;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenditureRepository extends JpaRepository<Expenditure, Long> {
    @Query("SELECT e FROM Expenditure e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.store " +
            "WHERE e.member = :member " +
            "AND e.expenditureDate >= :start AND e.expenditureDate < :end " +
            "ORDER BY e.expenditureDate DESC")
    List<Expenditure> findByMonth(
            @Param("member") Member member,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT e FROM Expenditure e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.store " +
            "WHERE e.id = :expenditureId AND e.member = :member")
    Optional<Expenditure> findByIdAndMember(
            @Param("expenditureId") Long expenditureId,
            @Param("member") Member member
    );
}
