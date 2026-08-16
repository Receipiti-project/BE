package com.receipiti.be.domain.member.entity;

import com.receipiti.be.domain.member.enums.SocialType;
import com.receipiti.be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_social_id_social_type",
                columnNames = {"social_id", "social_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "social_id", nullable = false)
    private Long socialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false)
    private SocialType socialType;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private String email;

    public Member update(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
        return this;
    }

}
