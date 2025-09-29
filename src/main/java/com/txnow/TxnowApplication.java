package com.txnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TxnowApplication {
    public static void main(String[] args) {
        SpringApplication.run(TxnowApplication.class, args);
    }
}