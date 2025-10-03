package com.txnow.application.exchange;

import static com.txnow.application.exchange.dto.ExchangeResult.ExchangeChartResult;
import static com.txnow.application.exchange.dto.ExchangeResult.ExchangeConvertResult;

import com.txnow.application.exchange.dto.ExchangeCommand;
import com.txnow.domain.exchange.exception.InvalidAmountException;
import com.txnow.domain.exchange.exception.InvalidCurrencyException;
import com.txnow.domain.exchange.model.ChartPeriod;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.DailyRate;
import com.txnow.domain.exchange.model.ExchangeRateCalculator;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        String periodCode = command.period();
        Currency baseCurrency = command.baseCurrency();
        Currency targetCurrency = command.targetCurrency();

        if (baseCurrency == null) {
            throw new InvalidCurrencyException(null, "Base currency is required");
        }
        if (targetCurrency == null) {
            throw new InvalidCurrencyException(null, "Target currency is required");
        }
        if (targetCurrency != Currency.KRW) {
            throw new InvalidCurrencyException(targetCurrency, "Target currency must be KRW");
        }

        ChartPeriod period = ChartPeriod.fromCode(periodCode);
        List<DailyRate> rates = exchangeRateProvider.getExchangeRateHistory(baseCurrency, period);

        return chartMapper.toChartResult(baseCurrency, targetCurrency, periodCode, rates);
    }

    /**
     * 환율 변환 계산을 수행합니다.
     */
    public ExchangeConvertResult convertExchangeRate(ExchangeCommand.ExchangeConvertCommand command) {
        Currency fromCurrency = command.from();
        Currency toCurrency = command.to();
        BigDecimal amount = command.amount();

        if (fromCurrency == null) {
            throw new InvalidCurrencyException(null, "From currency is required");
        }
        if (toCurrency == null) {
            throw new InvalidCurrencyException(null, "To currency is required");
        }
        if (amount == null) {
            throw new InvalidAmountException(null, "Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(amount, "Amount must be positive");
        }

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