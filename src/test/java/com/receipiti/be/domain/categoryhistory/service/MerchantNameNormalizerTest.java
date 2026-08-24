package com.receipiti.be.domain.categoryhistory.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MerchantNameNormalizerTest {

    private final MerchantNameNormalizer normalizer = new MerchantNameNormalizer();

    @Test
    void 가맹점명의_공백과_특수문자와_법인표현을_제거한다() {
        assertThat(normalizer.normalizeStoreName("(주) 스타벅스 강남점"))
                .isEqualTo("스타벅스강남점");
    }

    @Test
    void 지점명이_다른_가맹점을_동일한_브랜드로_정규화한다() {
        assertThat(normalizer.normalizeBrandName("스타벅스 강남점")).isEqualTo("스타벅스");
        assertThat(normalizer.normalizeBrandName("스타벅스 역삼점")).isEqualTo("스타벅스");
    }

    @Test
    void 지점_표현이_없으면_전체_가맹점명을_브랜드로_사용한다() {
        assertThat(normalizer.normalizeBrandName("투썸플레이스"))
                .isEqualTo("투썸플레이스");
    }
}
