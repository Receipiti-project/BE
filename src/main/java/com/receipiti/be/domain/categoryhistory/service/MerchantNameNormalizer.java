package com.receipiti.be.domain.categoryhistory.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MerchantNameNormalizer {

    private static final String CORPORATE_MARKER_REGEX = "(?i)\\(주\\)|주식회사|㈜";
    private static final String BRANCH_SUFFIX_REGEX = "\\s+[가-힣a-zA-Z0-9]+(?:지점|점)$";
    private static final String NON_ALPHANUMERIC_REGEX = "[^가-힣a-z0-9]";

    public String normalizeStoreName(String storeName) {
        return compact(removeCorporateMarker(storeName));
    }

    public String normalizeBrandName(String storeName) {
        String withoutCorporateMarker = removeCorporateMarker(storeName);
        String withoutBranchSuffix = withoutCorporateMarker.replaceFirst(BRANCH_SUFFIX_REGEX, "");
        return compact(withoutBranchSuffix);
    }

    private String removeCorporateMarker(String storeName) {
        return storeName.trim().replaceAll(CORPORATE_MARKER_REGEX, "").trim();
    }

    private String compact(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll(NON_ALPHANUMERIC_REGEX, "");
    }
}
