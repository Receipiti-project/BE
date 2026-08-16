package com.receipiti.be.domain.member.repository;

import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.member.enums.SocialType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySocialIdAndSocialType(Long socialId, SocialType socialType);
    Optional<Member> findBySocialId(Long socialId);

    @Modifying
    @Query(value = """
            INSERT INTO member (social_id, social_type, nickname, email, created_at, updated_at)
            VALUES (:socialId, :socialType, :nickname, :email, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                nickname = VALUES(nickname),
                email = VALUES(email),
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertSocialMember(
            @Param("socialId") Long socialId,
            @Param("socialType") String socialType,
            @Param("nickname") String nickname,
            @Param("email") String email);
}
