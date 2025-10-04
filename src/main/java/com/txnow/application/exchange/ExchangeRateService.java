package com.txnow.application.exchange;

import static com.txnow.application.exchange.dto.ExchangeResult.ExchangeChartResult;
import static com.txnow.application.exchange.dto.ExchangeResult.ExchangeConvertResult;

import com.txnow.application.exchange.dto.ExchangeCommand;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.DailyRate;
import com.txnow.domain.exchange.model.ExchangeRateCalculator;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeChartMapper chartMapper;

    /**
     * 환율 차트 데이터 조회
     */
    public ExchangeChartResult getExchangeRateChart(ExchangeCommand.ExchangeChartCommand command) {
        Currency baseCurrency = command.baseCurrency();
        Currency targetCurrency = command.targetCurrency();
        String startDateStr = command.startDate();
        String endDateStr = command.endDate();

        Assert.notNull(baseCurrency, "Base currency is required");
        Assert.notNull(targetCurrency, "Target currency is required");
        Assert.isTrue(targetCurrency == Currency.KRW, "Target currency must be KRW");
        Assert.hasText(startDateStr, "Start date is required");
        Assert.hasText(endDateStr, "End date is required");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(startDateStr, formatter);
        LocalDate endDate = LocalDate.parse(endDateStr, formatter);

        Assert.isTrue(!startDate.isAfter(endDate), "Start date must be before or equal to end date");

        List<DailyRate> rates = exchangeRateProvider.getExchangeRateHistory(baseCurrency, startDate, endDate);

        return chartMapper.toChartResult(baseCurrency, targetCurrency, startDateStr, endDateStr, rates);
    }

    /**
     * 환율 변환 계산을 수행합니다.
     */
    public ExchangeConvertResult convertExchangeRate(ExchangeCommand.ExchangeConvertCommand command) {
        Currency fromCurrency = command.from();
        Currency toCurrency = command.to();
        BigDecimal amount = command.amount();

        Assert.notNull(fromCurrency, "From currency is required");
        Assert.notNull(toCurrency, "To currency is required");
        Assert.notNull(amount, "Amount is required");
        Assert.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive");

        // 동일 통화 처리
        if (fromCurrency.equals(toCurrency)) {
            return new ExchangeConvertResult(
                command.amount(),
                BigDecimal.ONE,
                LocalDateTime.now()
            );
        }

        BigDecimal exchangeRate = exchangeRateProvider.getCurrentExchangeRate(command.from());

        BigDecimal convertedAmount = ExchangeRateCalculator.calculateConvertedAmount(command.amount(), exchangeRate);

        return new ExchangeConvertResult(
            convertedAmount,
            exchangeRate,
            LocalDateTime.now()
        );
    }
}