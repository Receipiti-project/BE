package com.receipiti.be.domain.expenditure.repository;

import com.receipiti.be.domain.expenditure.entity.Expenditure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenditureRepository extends JpaRepository<Expenditure, Long> {
}
