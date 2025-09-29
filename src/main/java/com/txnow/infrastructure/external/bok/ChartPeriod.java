package com.txnow.infrastructure.external.bok;

import java.time.LocalDate;

public enum ChartPeriod {
    ONE_DAY("1d", 1),
    ONE_WEEK("1w", 7),
    ONE_MONTH("1m", 30),
    THREE_MONTHS("3m", 90),
    ONE_YEAR("1y", 365);

    private final String code;
    private final int days;

    ChartPeriod(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public String getCode() {
        return code;
    }

    public int getDays() {
        return days;
    }

    public LocalDate getStartDate() {
        return LocalDate.now().minusDays(days);
    }

    /**
     * 종료일 계산
     */
    public LocalDate getEndDate() {
        return LocalDate.now();
    }

    /**
     * 필요한 데이터 개수 계산 (주말 고려)
     */
    public int getRequiredDataCount() {
        return switch (this) {
            case ONE_DAY -> 10;
            case ONE_WEEK -> 20;
            case ONE_MONTH -> 50;
            case THREE_MONTHS -> 100;
            case ONE_YEAR -> 400;
        };
    }

    public static ChartPeriod fromCode(String code) {
        for (ChartPeriod period : values()) {
            if (period.code.equals(code)) {
                return period;
            }
        }
        throw new IllegalArgumentException("Unknown period code: " + code);
    }
}