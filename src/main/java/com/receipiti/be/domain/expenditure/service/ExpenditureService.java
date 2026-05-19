package com.receipiti.be.domain.expenditure.service;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.exception.CategoryNotFoundException;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.response.DailyExpenditureGroup;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureElement;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureListResponse;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.enums.Currency;
import com.receipiti.be.domain.expenditure.enums.InputType;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.domain.store.repository.StoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenditureService {

    private final ExpenditureRepository expenditureRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    public ExpenditureCreateResponse createExpenditure(Member member, ExpenditureCreateRequest request){

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("존재하지 않는 카테고리입니다."));

        Store store = storeRepository.findByName(request.getStoreName())
                .orElseGet(() -> storeRepository.save(
                        Store.builder()
                                .name(request.getStoreName())
                                .build()
                ));

        Expenditure expenditure = Expenditure.builder()
                .member(member)
                .category(category)
                .store(store)
                .amount(request.getAmount())
                .expenditureDate(request.getExpenditureDate())
                .memo(request.getMemo())
                .currency(request.getCurrency() != null ? request.getCurrency(): Currency.KRW)
                .inputType(InputType.MANUAL)
                .build();

       Expenditure saved = expenditureRepository.save(expenditure);
       return new ExpenditureCreateResponse(
               saved.getId(),
               saved.getStore().getName(),
               saved.getAmount(),
               saved.getExpenditureDate(),
               saved.getMemo(),
               saved.getCurrency());
    }

    @Transactional(readOnly = true)
    public ExpenditureListResponse getExpenditureList(Member member, int year, int month){
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Expenditure> expenditures = expenditureRepository
                .findByMonth(member, startDateTime, endDateTime);

        // 월별 총 금액
        Long totalAmount = expenditures.stream()
                .mapToLong(Expenditure::getAmount)
                .sum();

        Map<LocalDate, List<Expenditure>> groupedByDate = expenditures.stream()
                .collect(Collectors.groupingBy(element -> element.getExpenditureDate().toLocalDate()));

        // 쪼갠 데이터를 일별 그룹 DTO 리스트로 가공 및 날짜 최신순 정렬
        List<DailyExpenditureGroup> dailyExpenditures = groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Expenditure> list = entry.getValue();

                    // 일별 총 금액
                    Long dailyTotal = list.stream().mapToLong(Expenditure::getAmount).sum();
                    
                    List<ExpenditureElement> elements = list.stream()
                            .map(exp -> ExpenditureElement.builder()
                                    .expenditureId(exp.getId())
                                    .categoryName(exp.getCategory().getName())
                                    .storeName(exp.getStore().getName())
                                    .amount(exp.getAmount())
                                    .expenditureDate(exp.getExpenditureDate())
                                    .memo(exp.getMemo())
                                    .currency(exp.getCurrency())
                                    .build())
                            .collect(Collectors.toList());

                    return new DailyExpenditureGroup(date, dailyTotal, elements);
                })
                .sorted((g1, g2) -> g2.getDate().compareTo(g1.getDate())) // 최근 날짜가 맨 위로 오게 정렬
                .collect(Collectors.toList());

        return new ExpenditureListResponse(totalAmount, dailyExpenditures);
    }
}
