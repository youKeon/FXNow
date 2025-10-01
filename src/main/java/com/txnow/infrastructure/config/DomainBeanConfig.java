package com.txnow.infrastructure.config;

import com.txnow.domain.exchange.model.ExchangeRateCalculator;
import com.txnow.domain.exchange.model.ExchangeRateChart;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain Layer 객체들의 Bean 등록
 * Domain은 순수 POJO로 유지하고, Bean 등록은 Infrastructure에서 담당
 */
@Configuration
public class DomainBeanConfig {

    /**
     * ExchangeRateCalculator Bean 등록
     */
    @Bean
    public ExchangeRateCalculator exchangeRateCalculator() {
        return new ExchangeRateCalculator();
    }

    /**
     * ExchangeRateChart Bean 등록
     */
    @Bean
    public ExchangeRateChart exchangeRateChart(ExchangeRateCalculator calculator) {
        return new ExchangeRateChart(calculator);
    }
}
