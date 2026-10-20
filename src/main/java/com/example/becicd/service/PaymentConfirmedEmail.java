package com.example.becicd.service;

import lombok.Value;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Immutable snapshot; the mail listener never accesses JPA entities after commit. */
@Value
public class PaymentConfirmedEmail {
    Long bookingId;
    String recipient;
    String customerName;
    String fieldName;
    LocalDate date;
    LocalTime start;
    LocalTime end;
    BigDecimal amount;
}
