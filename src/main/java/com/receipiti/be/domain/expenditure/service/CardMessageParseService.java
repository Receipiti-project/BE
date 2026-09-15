package com.receipiti.be.domain.expenditure.service;

import com.receipiti.be.domain.expenditure.dto.request.CardMessageParseRequest;
import com.receipiti.be.domain.expenditure.dto.response.CardNotificationAnalysisResponse;
import com.receipiti.be.domain.expenditure.repository.CardMessageParseRequestRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardMessageParseService {

    private static final int MAX_MESSAGE_LENGTH = 100;
    private static final int MAX_EXTERNAL_ID_LENGTH = 100;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?<![\\d,])(\\d{1,3}(?:,\\d{3})+|\\d+)\\s*원");
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("(?:(\\d{4})[./-])?(\\d{1,2})[./-](\\d{1,2})\\s+(\\d{1,2}):(\\d{2})");
    private static final Pattern COMPANY_PATTERN = Pattern.compile("\\[([^]\\r\\n]{1,20}?(?:카드|CARD))]", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS_PATTERN = Pattern.compile("승인취소|승인|취소|사용", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPACT_COMPANY_PATTERN = Pattern.compile(
            "(신한|KB국민|국민|삼성|현대|롯데|하나|우리|NH농협|농협|BC|비씨)"
                    + "(?:카드|[\\d*.-]+(?:체크|신용)?|체크|신용)(?:승인취소|승인|취소|사용)",
            Pattern.CASE_INSENSITIVE
    );
    private static final List<String> CARD_COMPANIES = List.of(
            "신한카드", "KB국민카드", "국민카드", "삼성카드", "현대카드", "롯데카드",
            "하나카드", "우리카드", "NH농협카드", "농협카드", "BC카드", "비씨카드", "카카오뱅크", "토스뱅크"
    );

    private final CardMessageParseRequestRepository requestRepository;

    @Transactional
    public CardNotificationAnalysisResponse parse(Member member, CardMessageParseRequest request) {
        String externalId = request.externalId().trim();
        if (request.message().length() > MAX_MESSAGE_LENGTH || externalId.length() > MAX_EXTERNAL_ID_LENGTH) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
        if (requestRepository.existsByMemberAndExternalId(member, externalId)) {
            throw new GeneralException(GeneralErrorCode.CARD_MESSAGE_DUPLICATE);
        }

        String normalized = normalize(request.message());
        ParsedMessage parsed = extract(normalized, request.receivedAt());

        try {
            requestRepository.saveAndFlush(com.receipiti.be.domain.expenditure.entity.CardMessageParseRequest.builder()
                    .member(member)
                    .externalId(externalId)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(GeneralErrorCode.CARD_MESSAGE_DUPLICATE);
        }

        return new CardNotificationAnalysisResponse(
                true,
                parsed.cardCompany(),
                parsed.storeName(),
                parsed.amount(),
                parsed.paymentDateTime().toString(),
                "KRW",
                parsed.cancelled() ? "CANCELLED" : "APPROVED",
                1.0
        );
    }

    @Transactional
    public CardNotificationAnalysisResponse parseRaw(Member member, String message) {
        if (message == null || message.isBlank()) {
            throw new GeneralException(GeneralErrorCode.CARD_MESSAGE_INFORMATION_INSUFFICIENT);
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
        String normalized = normalize(message);
        return parse(member, new CardMessageParseRequest(
                message,
                LocalDateTime.now(),
                "message-sha256-" + sha256(normalized)
        ));
    }

    private ParsedMessage extract(String message, LocalDateTime receivedAt) {
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(message);
        Matcher dateMatcher = DATE_TIME_PATTERN.matcher(message);
        Matcher statusMatcher = STATUS_PATTERN.matcher(message);
        String cardCompany = findCardCompany(message);

        if (cardCompany == null || !amountMatcher.find() || !dateMatcher.find() || !statusMatcher.find()) {
            throw new GeneralException(GeneralErrorCode.CARD_MESSAGE_UNSUPPORTED);
        }

        long amount;
        LocalDateTime paymentDateTime;
        try {
            amount = Long.parseLong(amountMatcher.group(1).replace(",", ""));
            int year = dateMatcher.group(1) == null ? receivedAt.getYear() : Integer.parseInt(dateMatcher.group(1));
            paymentDateTime = LocalDateTime.of(
                    year,
                    Integer.parseInt(dateMatcher.group(2)),
                    Integer.parseInt(dateMatcher.group(3)),
                    Integer.parseInt(dateMatcher.group(4)),
                    Integer.parseInt(dateMatcher.group(5))
            );
            if (dateMatcher.group(1) == null && paymentDateTime.isAfter(receivedAt.plusDays(1))) {
                paymentDateTime = paymentDateTime.minusYears(1);
            }
        } catch (NumberFormatException | DateTimeException exception) {
            throw new GeneralException(GeneralErrorCode.CARD_MESSAGE_INFORMATION_INSUFFICIENT);
        }

        String storeName = extractStoreName(message, cardCompany, amountMatcher, dateMatcher, statusMatcher);
        if (amount <= 0 || storeName == null) {
            throw new GeneralException(GeneralErrorCode.CARD_MESSAGE_INFORMATION_INSUFFICIENT);
        }

        String status = statusMatcher.group().toLowerCase(Locale.ROOT);
        return new ParsedMessage(cardCompany, storeName, amount, paymentDateTime, status.contains("취소"));
    }

    private String extractStoreName(
            String message,
            String cardCompany,
            Matcher amountMatcher,
            Matcher dateMatcher,
            Matcher statusMatcher
    ) {
        String remaining = message;
        remaining = removeRange(remaining, amountMatcher.start(), amountMatcher.end());
        remaining = DATE_TIME_PATTERN.matcher(remaining).replaceFirst(" ");
        remaining = COMPACT_COMPANY_PATTERN.matcher(remaining).replaceFirst(" ");
        remaining = STATUS_PATTERN.matcher(remaining).replaceFirst(" ");
        remaining = COMPANY_PATTERN.matcher(remaining).replaceFirst(" ");
        remaining = remaining.replace(cardCompany, " ")
                .replaceAll("\\[Web발신]", " ")
                .replaceAll("\\[\\s*]", " ")
                .replaceAll("\\[(?:신한|KB|국민|삼성|현대|롯데|하나|우리|NH|농협|BC|비씨)[^]]*]", " ")
                .replaceAll("[가-힣]{1,4}[*][가-힣]{1,4}\\s*\\(\\d{3,4}\\)", " ")
                .replaceAll("[가-힣]{1,4}[*][가-힣]{1,4}", " ")
                .replaceAll("\\(금액\\)", " ")
                .replaceAll("(?i)일시불|누적|잔액|체크|본인|해외|국내|결제", " ")
                .replaceAll("(?<!\\d)\\d{2,4}[-*]\\d{2,4}(?!\\d)", " ")
                .replaceAll("[*]{2,}|[-|:/]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return remaining.isBlank() ? null : remaining;
    }

    private String removeRange(String value, int start, int end) {
        return value.substring(0, start) + " " + value.substring(end);
    }

    private String findCardCompany(String message) {
        Matcher matcher = COMPANY_PATTERN.matcher(message);
        if (matcher.find()) {
            String bracketCompany = matcher.group(1).trim();
            return CARD_COMPANIES.stream()
                    .filter(company -> company.equalsIgnoreCase(bracketCompany))
                    .findFirst()
                    .orElse(null);
        }
        Matcher compactMatcher = COMPACT_COMPANY_PATTERN.matcher(message);
        if (compactMatcher.find()) {
            return canonicalCompanyName(compactMatcher.group(1));
        }
        return CARD_COMPANIES.stream().filter(message::contains).findFirst().orElse(null);
    }

    private String canonicalCompanyName(String companyPrefix) {
        return switch (companyPrefix.toUpperCase(Locale.ROOT)) {
            case "신한" -> "신한카드";
            case "KB국민", "국민" -> "KB국민카드";
            case "삼성" -> "삼성카드";
            case "현대" -> "현대카드";
            case "롯데" -> "롯데카드";
            case "하나" -> "하나카드";
            case "우리" -> "우리카드";
            case "NH농협", "농협" -> "NH농협카드";
            case "BC", "비씨" -> "BC카드";
            default -> throw new GeneralException(GeneralErrorCode.CARD_MESSAGE_UNSUPPORTED);
        };
    }

    private String normalize(String message) {
        return message.replace('\u00a0', ' ')
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private record ParsedMessage(
            String cardCompany,
            String storeName,
            long amount,
            LocalDateTime paymentDateTime,
            boolean cancelled
    ) {
    }
}
