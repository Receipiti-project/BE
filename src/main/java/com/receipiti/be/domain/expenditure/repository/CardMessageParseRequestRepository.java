package com.receipiti.be.domain.expenditure.repository;

import com.receipiti.be.domain.expenditure.entity.CardMessageParseRequest;
import com.receipiti.be.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardMessageParseRequestRepository extends JpaRepository<CardMessageParseRequest, Long> {

    boolean existsByMemberAndExternalId(Member member, String externalId);
}
