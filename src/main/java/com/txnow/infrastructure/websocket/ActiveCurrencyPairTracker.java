package com.txnow.infrastructure.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ActiveCurrencyPairTracker {

    private final Set<String> activeCurrencyPairs = ConcurrentHashMap.newKeySet();

    public void addActivePair(String from, String to) {
        String pairKey = createPairKey(from, to);
        activeCurrencyPairs.add(pairKey);
    }

    public void removeActivePair(String from, String to) {
        String pairKey = createPairKey(from, to);
        activeCurrencyPairs.remove(pairKey);
    }

    public Set<String> getActivePairs() {
        return Set.copyOf(activeCurrencyPairs);
    }

    private String createPairKey(String from, String to) {
        return from + "/" + to;
    }
}