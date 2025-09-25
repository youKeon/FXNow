package com.txnow.infrastructure.external.bok;

import com.txnow.domain.exchange.model.Currency;

import java.util.Map;

public final class BokCurrencyMapping {

    // 한국은행 API 통화 코드와 Currency enum 매핑
    private static final Map<Currency, String> CURRENCY_CODE_MAP = Map.of(
        Currency.USD, "0000001",  // 원/미국달러
        Currency.EUR, "0000003",  // 원/유로
        Currency.JPY, "0000002",  // 원/일본엔
        Currency.CNY, "0000027",  // 원/중국위안
        Currency.GBP, "0000012"   // 원/영국파운드
    );

    // Currency를 한국은행 API 코드로 변환
    public static String getBokCode(Currency currency) {
        return CURRENCY_CODE_MAP.get(currency);
    }

    // 한국은행 API에서 지원하는 통화인지 확인
    public static boolean isSupported(Currency currency) {
        return CURRENCY_CODE_MAP.containsKey(currency);
    }

    // 지원하는 모든 통화 반환
    public static Currency[] getSupportedCurrencies() {
        return CURRENCY_CODE_MAP.keySet().toArray(new Currency[0]);
    }

    private BokCurrencyMapping() {
        // Utility class
    }
}