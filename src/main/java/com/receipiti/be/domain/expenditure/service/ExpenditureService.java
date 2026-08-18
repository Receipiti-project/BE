package com.receipiti.be.domain.expenditure.service;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureUpdateRequest;
import com.receipiti.be.domain.expenditure.dto.response.DailyExpenditureGroup;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureDetailResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureElement;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureListResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureUpdateResponse;
import com.receipiti.be.domain.expenditure.dto.response.OcrResponse;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.enums.Currency;
import com.receipiti.be.domain.expenditure.enums.InputType;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.domain.store.repository.StoreRepository;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenditureService {

    private final ExpenditureRepository expenditureRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    private final NaverOcrHandler naverOcrHandler;

    public ExpenditureCreateResponse createExpenditure(Member member, ExpenditureCreateRequest request){

        Category category = categoryRepository.findAccessibleCategory(request.getCategoryId(), member)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.CATEGORY_NOT_FOUND));

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
    public ExpenditureListResponse getExpenditureList(Member member, int year, int month) {
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

    @Transactional(readOnly = true)
    public ExpenditureDetailResponse getExpenditureDetail(Member member, Long expenditureId) {
        Expenditure expenditure = expenditureRepository.findByIdAndMember(expenditureId, member)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.EXPENDITURE_NOT_FOUND));

        return ExpenditureDetailResponse.builder()
                .expenditureId(expenditure.getId())
                .categoryId(expenditure.getCategory().getId())
                .categoryName(expenditure.getCategory().getName())
                .storeName(expenditure.getStore().getName())
                .amount(expenditure.getAmount())
                .expenditureDate(expenditure.getExpenditureDate())
                .memo(expenditure.getMemo())
                .currency(expenditure.getCurrency())
                .inputType(expenditure.getInputType())
                .createdAt(expenditure.getCreatedAt())
                .build();
    }

    @Transactional
    public ExpenditureUpdateResponse updateExpenditure(Member member, Long id, ExpenditureUpdateRequest request) {
        Expenditure expenditure = expenditureRepository.findByIdAndMember(id, member)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.EXPENDITURE_NOT_FOUND));

        // 카테고리 수정
        Category category = expenditure.getCategory(); // 변경 없으면 기존 값 유지
        if (request.getCategoryId() != null && !request.getCategoryId().equals(category.getId())) {
            category = categoryRepository.findAccessibleCategory(request.getCategoryId(), member)
                    .orElseThrow(() -> new GeneralException(GeneralErrorCode.CATEGORY_NOT_FOUND));
        }

        // 가게명 수정
        Store store = expenditure.getStore();
        if (request.getStoreName() != null && !request.getStoreName().trim().isEmpty()) {
            String newStoreName = request.getStoreName().trim();
            if (!newStoreName.equals(store.getName())) { // 기존 가게명과 다를 때만 실행
                store = storeRepository.findByName(newStoreName)
                        .orElseGet(() -> storeRepository.save(
                                Store.builder()
                                        .name(newStoreName)
                                        .build()
                        ));
            }
        }

        // 엔티티에 값 던져서 변경 감지
        expenditure.update(
                category,
                store,
                request.getAmount(),
                request.getExpenditureDate(),
                request.getMemo(),
                request.getCurrency()
        );

        // 최종 수정 완료된 데이터 응답 DTO로 변환하여 반환
        return new ExpenditureUpdateResponse(
                expenditure.getId(),
                expenditure.getStore().getName(),
                expenditure.getAmount(),
                expenditure.getExpenditureDate(),
                expenditure.getMemo(),
                expenditure.getCurrency()
        );
    }

    @Transactional
    public void deleteExpenditure(Member member, Long id) {
        Expenditure expenditure = expenditureRepository.findByIdAndMember(id, member)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.EXPENDITURE_NOT_FOUND));

        expenditureRepository.delete(expenditure);
    }

    public OcrResponse extractTextFromReceipt(MultipartFile file) {
        return naverOcrHandler.executeOcr(file);
    }
}
