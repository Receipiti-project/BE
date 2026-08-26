package com.receipiti.be.domain.expenditure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.expenditure.dto.response.ConsumptionRouteResponse;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumptionRouteServiceTest {

    @Mock
    private ExpenditureRepository expenditureRepository;

    @InjectMocks
    private ConsumptionRouteService consumptionRouteService;

    private Member member;
    private Category category;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).build();
        category = Category.builder().id(1L).name("식비").build();
    }

    @Test
    void 좌표가_있는_지출을_시간순_소비_동선으로_반환한다() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        Expenditure first = expenditure(
                1L,
                store(1L, "강남역 카페", "37.4979000", "127.0276000"),
                12_000L,
                date.atTime(9, 30)
        );
        Expenditure second = expenditure(
                2L,
                store(2L, "역삼 식당", "37.5007000", "127.0365000"),
                35_000L,
                date.atTime(18, 45)
        );
        when(expenditureRepository.findRouteByDate(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(first, second));

        ConsumptionRouteResponse response = consumptionRouteService.getRoute(member, date);

        assertThat(response.visitedPlaceCount()).isEqualTo(2);
        assertThat(response.unmappedExpenditureCount()).isZero();
        assertThat(response.totalExpenditure()).isEqualTo(47_000L);
        assertThat(response.estimatedDistanceMeters()).isBetween(800L, 900L);
        assertThat(response.places()).extracting(place -> place.sequence())
                .containsExactly(1, 2);
        assertThat(response.places()).extracting(place -> place.storeName())
                .containsExactly("강남역 카페", "역삼 식당");
    }

    @Test
    void 좌표가_없는_지출은_총액에는_포함하고_동선에서는_제외한다() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        Expenditure mapped = expenditure(
                1L,
                store(1L, "강남역 카페", "37.4979000", "127.0276000"),
                12_000L,
                date.atTime(9, 30)
        );
        Expenditure unmapped = expenditure(
                2L,
                Store.builder().id(2L).name("과거 가맹점").build(),
                5_500L,
                date.atTime(14, 15)
        );
        when(expenditureRepository.findRouteByDate(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(mapped, unmapped));

        ConsumptionRouteResponse response = consumptionRouteService.getRoute(member, date);

        assertThat(response.visitedPlaceCount()).isEqualTo(1);
        assertThat(response.unmappedExpenditureCount()).isEqualTo(1);
        assertThat(response.totalExpenditure()).isEqualTo(17_500L);
        assertThat(response.estimatedDistanceMeters()).isZero();
        assertThat(response.places()).hasSize(1);
    }

    private Expenditure expenditure(
            Long id,
            Store store,
            Long amount,
            LocalDateTime expenditureDate
    ) {
        return Expenditure.builder()
                .id(id)
                .member(member)
                .category(category)
                .store(store)
                .amount(amount)
                .expenditureDate(expenditureDate)
                .build();
    }

    private Store store(Long id, String name, String latitude, String longitude) {
        return Store.builder()
                .id(id)
                .name(name)
                .kakaoPlaceId("place-" + id)
                .latitude(new BigDecimal(latitude))
                .longitude(new BigDecimal(longitude))
                .build();
    }
}
