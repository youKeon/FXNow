package com.txnow.infrastructure.persistence.repository;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import com.txnow.infrastructure.persistence.entity.ExchangeRateHistoryJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExchangeRateHistoryRepositoryImpl implements ExchangeRateHistoryRepository {

    private final ExchangeRateHistoryJpaRepository jpaRepository;

    @Override
    public List<ExchangeRateHistory> findByCurrencyAndTimestampBetween(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {
        return jpaRepository.findByCurrencyAndTimestampBetweenOrderByTimestampAsc(
                currency, startTime, endTime)
            .stream()
            .map(ExchangeRateHistoryJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public ExchangeRateHistory findExchangeRateByTimestamp(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {
        ExchangeRateHistoryJpaEntity entity = jpaRepository
            .findFirstByCurrencyAndTimestampBetweenOrderByTimestampDesc(
                currency, startTime, endTime);
        return entity != null ? entity.toDomain() : null;
    }

    @Override
    public void save(ExchangeRateHistory history) {
        ExchangeRateHistoryJpaEntity jpaEntity = ExchangeRateHistoryJpaEntity.fromDomain(history);
        jpaRepository.save(jpaEntity);
    }
}
