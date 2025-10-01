package com.txnow.domain.exchange.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ExchangeRateCalculator {

    /**
     * 금액과 환율을 기반으로 변환된 금액을 계산합니다.
     * @param amount 변환할 금액
     * @param exchangeRate 적용할 환율
     * @return 변환된 금액 (소수점 둘째 자리 반올림)
     */
    public BigDecimal calculateConvertedAmount(BigDecimal amount, BigDecimal exchangeRate) {
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 환율 변동률을 계산합니다.
     * @param currentRate 현재 환율
     * @param previousRate 이전 환율
     * @return 변동률 (백분율)
     */
    public BigDecimal calculateChangePercentage(BigDecimal currentRate, BigDecimal previousRate) {
        if (previousRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentRate.subtract(previousRate)
            .divide(previousRate, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }
}