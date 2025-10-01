package com.txnow.application.exchange;

import static com.txnow.application.exchange.dto.ExchangeRateChartResult.*;

import com.txnow.application.exchange.dto.ConvertExchangeRateCommand;
import com.txnow.application.exchange.dto.ConvertExchangeRateResult;
import com.txnow.application.exchange.dto.ExchangeRateChartResult;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateCalculator;
import com.txnow.domain.exchange.model.ExchangeRateChart;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import com.txnow.domain.exchange.model.HistoricalRate;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import com.txnow.infrastructure.external.bok.ChartPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeRateCalculator calculator;
    private final ExchangeRateChart chart;
    private final ExchangeRateHistoryRepository historyRepository;

    /**
     * 환율 차트 데이터 조회
     */
    public ExchangeRateChartResult getExchangeRateChart(
        Currency baseCurrency,
        Currency targetCurrency,
        String periodCode
    ) {
        Assert.isTrue(targetCurrency == Currency.KRW, "Target currency must be KRW");

        log.info("Fetching chart data for {}/{} period: {}", baseCurrency, targetCurrency,
            periodCode);

        // 1일 조회는 DB의 시간대별 데이터 사용
        if ("1d".equals(periodCode)) {
            return getOneDayChartFromHistory(baseCurrency, targetCurrency);
        }

        ChartPeriod period = ChartPeriod.fromCode(periodCode);
        List<HistoricalRate> historicalRates = exchangeRateProvider.getExchangeRateHistory(baseCurrency, period);

        // 차트 생성
        return chart.createChartResult(baseCurrency, targetCurrency, periodCode, historicalRates);
    }

    /**
     * DB에 저장된 시간대별 환율로 1일 차트 생성
     */
    private ExchangeRateChartResult getOneDayChartFromHistory(
        Currency baseCurrency,
        Currency targetCurrency
    ) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);

        List<ExchangeRateHistory> historyList = historyRepository
            .findByCurrencyAndTimestampBetweenOrderByTimestampAsc(baseCurrency, startTime, endTime);

        // DB에 데이터가 없으면 외부 API 사용
        if (historyList.isEmpty()) {
            log.warn("No history data found for {} in last 24 hours, falling back to external API", baseCurrency);
            ChartPeriod period = ChartPeriod.fromCode("1d");
            List<HistoricalRate> historicalRates = exchangeRateProvider.getExchangeRateHistory(baseCurrency, period);
            return chart.createChartResult(baseCurrency, targetCurrency, "1d", historicalRates);
        }

        // 차트 데이터 포인트 생성
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        List<ChartDataPoint> chartData = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();

        for (int i = 0; i < historyList.size(); i++) {
            ExchangeRateHistory history = historyList.get(i);
            rates.add(history.getRate());

            BigDecimal dayChange = BigDecimal.ZERO;
            if (i > 0) {
                BigDecimal prevRate = historyList.get(i - 1).getRate();
                dayChange = calculator.calculateChangePercentage(history.getRate(), prevRate);
            }

            chartData.add(new ChartDataPoint(
                history.getTimestamp().format(dateFormatter),
                history.getTimestamp().format(timeFormatter),
                history.getRate(),
                dayChange
            ));
        }

        // 통계 계산
        BigDecimal high = rates.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal low = rates.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal sum = rates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(new BigDecimal(rates.size()), 4,
            java.math.RoundingMode.HALF_UP);

        ChartStatistics statistics =
            new ChartStatistics(high, low, average);

        // 현재값 및 변동
        BigDecimal currentRate = rates.getLast();
        BigDecimal change = rates.size() > 1
            ? currentRate.subtract(rates.getFirst())
            : BigDecimal.ZERO;
        BigDecimal changePercent = rates.size() > 1
            ? calculator.calculateChangePercentage(currentRate, rates.getFirst())
            : BigDecimal.ZERO;

        return new ExchangeRateChartResult(
            baseCurrency,
            targetCurrency,
            "1d",
            currentRate,
            change,
            changePercent.setScale(2, java.math.RoundingMode.HALF_UP),
            LocalDateTime.now(),
            chartData,
            statistics
        );
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
            return new ConvertExchangeRateResult(
                command.amount(),
                BigDecimal.ONE,
                LocalDateTime.now()
            );
        }

        BigDecimal exchangeRate = exchangeRateProvider.getCurrentExchangeRate(command.from());

        BigDecimal convertedAmount = calculator.calculateConvertedAmount(command.amount(), exchangeRate);

        return new ConvertExchangeRateResult(
            convertedAmount,
            exchangeRate,
            LocalDateTime.now()
        );
    }
}