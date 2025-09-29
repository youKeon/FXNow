package com.txnow.infrastructure.websocket;

import com.txnow.infrastructure.websocket.dto.CurrencyPairSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ExchangeRateWebSocketController {

    private final ActiveCurrencyPairTracker currencyPairTracker;

    @MessageMapping("/subscribe")
    public void subscribeToRate(@Payload CurrencyPairSubscription subscription) {
        currencyPairTracker.addActivePair(subscription.from(), subscription.to());
    }

    @MessageMapping("/unsubscribe")
    public void unsubscribeFromRate(@Payload CurrencyPairSubscription subscription) {
        currencyPairTracker.removeActivePair(subscription.from(), subscription.to());
    }
}