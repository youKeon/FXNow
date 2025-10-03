package com.txnow.domain.exchange.model;

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
     * 필요한 데이터 개수 계산 (주말 및 공휴일 고려)
     */
    public int getRequiredDataCount() {
        LocalDate startDate = getStartDate();
        LocalDate endDate = getEndDate();

        // 주말 제외 계산
        long businessDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek().getValue() <= 5) { // 월~금
                businessDays++;
            }
            current = current.plusDays(1);
        }

        // 공휴일은 대략 영업일의 10% 정도로 추정 (한국 공휴일 약 15일/년)
        long estimatedHolidays = (long) (businessDays * 0.1);
        long estimatedBusinessDays = businessDays - estimatedHolidays;

        // 20% 여유분 추가 (API 호출 실패 방지)
        int resultCount = (int) Math.ceil(estimatedBusinessDays * 1.2);

        // 최소 1개, 최대 100개로 제한
        return Math.max(1, Math.min(100, resultCount));
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
