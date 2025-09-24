package com.txnow.domain.exchange.model;

public enum Currency {
    USD("USD", "Dollar", "$", 2),
    EUR("EUR", "Euro", "€", 2),
    JPY("JPY", "Yen", "¥", 0),
    KRW("KRW", "Won", "₩", 0),
    CNY("CNY", "Yuan", "¥", 2),
    GBP("GBP", "Pound", "£", 2);

    private final String code;
    private final String name;
    private final String symbol;
    private final int decimalPlaces;

    Currency(String code, String name, String symbol, int decimalPlaces) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}