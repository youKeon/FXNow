package com.txnow.config;

import com.txnow.domain.exchange.repository.ExchangeRateRepository;
import com.txnow.domain.exchange.service.ExchangeRateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public ExchangeRateService exchangeRateService(ExchangeRateRepository repository) {
        return new ExchangeRateService(repository);
    }
}