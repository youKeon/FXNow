package com.txnow.infrastructure.websocket.dto;

public record CurrencyPairSubscription(
    String from,
    String to
) {
}