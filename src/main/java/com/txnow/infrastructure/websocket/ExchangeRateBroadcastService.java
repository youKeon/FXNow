package com.txnow.infrastructure.websocket;

import com.txnow.infrastructure.websocket.dto.ExchangeRateUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastExchangeRateUpdate(String from, String to, BigDecimal rate) {
        ExchangeRateUpdateMessage message = new ExchangeRateUpdateMessage(
                from,
                to,
                rate,
                LocalDateTime.now()
        );

        String destination = "/topic/exchange-rates/" + from + "/" + to;

        messagingTemplate.convertAndSend(destination, message);

        messagingTemplate.convertAndSend("/topic/exchange-rates", message);
    }
}