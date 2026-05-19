package com.receipiti.be.domain.expenditure.service;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.category.repository.CategoryRepository;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.enums.Currency;
import com.receipiti.be.domain.expenditure.enums.InputType;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.member.repository.MemberRepository;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenditureService {

    private final ExpenditureRepository expenditureRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    public ExpenditureCreateResponse createExpenditure(Member member, ExpenditureCreateRequest request){
        System.out.println("categoryId: " + request.getCategoryId());
        System.out.println("storeName: " + request.getStoreName());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다"));

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
}
