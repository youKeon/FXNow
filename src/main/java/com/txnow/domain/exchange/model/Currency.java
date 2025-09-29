package com.txnow.domain.exchange.model;

public enum Currency {
    USD("USD", "Dollar", "$", 2, "0000001"),
    EUR("EUR", "Euro", "€", 2, "0000003"),
    JPY("JPY", "Yen", "¥", 0, "0000002"),
    KRW("KRW", "Won", "₩", 0, null),
    CNY("CNY", "Yuan", "¥", 2, "0000027"),
    GBP("GBP", "Pound", "£", 2, "0000012");

    private final String code;
    private final String name;
    private final String symbol;
    private final int decimalPlaces;
    private final String bokCode;

    Currency(String code, String name, String symbol, int decimalPlaces, String bokCode) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
        this.bokCode = bokCode;
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

    public String getBokCode() {
        return bokCode;
    }

    public boolean isBokSupported() {
        return bokCode != null;
    }
}