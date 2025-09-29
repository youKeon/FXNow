package com.txnow.application.exchange;

import com.txnow.application.exchange.dto.ConvertExchangeRateCommand;
import com.txnow.application.exchange.dto.ConvertExchangeRateResult;
import com.txnow.application.exchange.dto.ExchangeRateChartResult;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateCalculator;
import com.txnow.domain.exchange.model.ExchangeRateChart;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.infrastructure.external.bok.ChartPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeRateCalculator calculator;
    private final ExchangeRateChart chart;

    /**
     * 환율 차트 데이터 조회
     */
    public ExchangeRateChartResult getExchangeRateChart(
        Currency baseCurrency,
        Currency targetCurrency,
        String periodCode
    ) {
        // 차트 검증
        Assert.isTrue(chart.isValidChartTargetCurrency(targetCurrency), "Target currency must be KRW");
        ChartPeriod period = ChartPeriod.fromCode(periodCode);

        log.info("Fetching chart data for {}/{} period: {}", baseCurrency, targetCurrency, periodCode);

        var response = exchangeRateProvider.getExchangeRateHistory(baseCurrency, period);

        // 차트 생성
        return chart.createChartResult(baseCurrency, targetCurrency, periodCode, response.get());
    }

    /**
     * 환율 변환 계산을 수행합니다.
     */
    public ConvertExchangeRateResult convertExchangeRate(ConvertExchangeRateCommand command) {
        // 기본 입력 검증
        Currency fromCurrency = command.from();
        Currency toCurrency = command.to();
        BigDecimal amount = command.amount();

        Assert.notNull(fromCurrency, "From currency is required");
        Assert.notNull(toCurrency, "To currency is required");
        Assert.notNull(amount, "Amount is required");
        Assert.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive");

        // 동일 통화 처리
        if (fromCurrency.equals(toCurrency)) {
            return new ConvertExchangeRateResult(command.amount(), LocalDateTime.now());
        }

        BigDecimal exchangeRate = exchangeRateProvider.getCurrentExchangeRate(command.from())
            .orElseThrow(() -> new IllegalArgumentException("Exchange rate not available"));

        BigDecimal convertedAmount = calculator.calculateConvertedAmount(command.amount(), exchangeRate);

        return new ConvertExchangeRateResult(convertedAmount, LocalDateTime.now());
    }
}