package com.txnow.infrastructure.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateUpdateMessage {
    private String from;
    private String to;
    private BigDecimal rate;
    private LocalDateTime timestamp;
}