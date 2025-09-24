package com.txnow.domain.exchange.model;

import java.util.Objects;

public record CurrencyPair(Currency base, Currency target) {

    public CurrencyPair {
        Objects.requireNonNull(base, "Base currency cannot be null");
        Objects.requireNonNull(target, "Target currency cannot be null");

        if (base == target) {
            throw new IllegalArgumentException("Base and target currencies cannot be the same");
        }
    }

    public String getPairCode() {
        return base.getCode() + "/" + target.getCode();
    }

    public CurrencyPair reverse() {
        return new CurrencyPair(target, base);
    }
}